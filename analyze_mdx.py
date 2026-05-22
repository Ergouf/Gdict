import struct
import os
import sys

files = [
    r"d:\workspace\Gdict\Cambridge_English_Pronouncing_Dictionary_18th.mdx",
    r"d:\workspace\Gdict\Collins COBUILD Advanced English Dictionary Online.mdx",
]

for fpath in files:
    if not os.path.exists(fpath):
        print(f"NOT FOUND: {fpath}")
        continue
    
    fname = os.path.basename(fpath)
    fsize = os.path.getsize(fpath)
    
    with open(fpath, 'rb') as f:
        data = f.read()
    
    print("=" * 70)
    print(f"FILE: {fname} ({fsize} bytes)")
    print("=" * 70)
    
    # Read headerLen as big-endian uint32
    b0, b1, b2, b3 = data[0], data[1], data[2], data[3]
    header_len_be = (b0 << 24) | (b1 << 16) | (b2 << 8) | b3
    header_len_le = (b3 << 24) | (b2 << 16) | (b1 << 8) | b0
    
    print(f"\nFirst 4 bytes: {b0:02X} {b1:02X} {b2:02X} {b3:02X}")
    print(f"  As BE uint32: {header_len_be}")
    print(f"  As LE uint32: {header_len_le}")
    
    # Try to decode header as UTF-16LE
    hdr_end = 4 + header_len_be + 4
    try:
        header_xml = data[4:4+header_len_be].decode('utf-16-le', errors='replace')
        print(f"\n--- Header XML ({header_len_be} chars, ends at offset {hdr_end}) ---")
        print(header_xml[:500])
        if len(header_xml) > 500:
            print(f"  ... ({len(header_xml)} total chars)")
        
        # Check if header looks complete
        has_closing = '</Dictionary>' in header_xml
        print(f"\n  Has closing </Dictionary>: {has_closing}")
        print(f"  Last 50 chars: ...{repr(header_xml[-50:])}")
    except Exception as e:
        print(f"Error decoding header: {e}")
    
    # Show hex dump around the supposed end of header
    print(f"\n--- Hex around offset {hdr_end-10} to {hdr_end+60} ---")
    start = max(0, hdr_end - 10)
    end = min(len(data), hdr_end + 60)
    for i in range(start, end, 16):
        hex_part = ' '.join(f'{data[j]:02X}' for j in range(i, min(i+16, end)))
        ascii_part = ''.join(chr(data[j]) if 32 <= data[j] < 127 else '.' for j in range(i, min(i+16, end)))
        print(f"  {i:6d}: {hex_part:<48s}  {ascii_part}")
    
    # Now try parsing keyword section at various offsets
    for label, off in [("at_hdr_end", hdr_end), ("at_256", 260), ("at_300", 300)]:
        if off + 48 <= len(data):
            p = off
            # V2.0 style: 5 x int64 + 1 x int32
            vals_v2 = []
            pos = p
            for _ in range(5):
                if pos + 8 <= len(data):
                    v = struct.unpack('>Q', data[pos:pos+8])[0]
                    vals_v2.append(v)
                    pos += 8
                else:
                    vals_v2.append(-1)
            if pos + 4 <= len(data):
                cs = struct.unpack('>I', data[pos:pos+4])[0]
                vals_v2.append(cs)
            
            # V1.2 style: 4 x int32
            vals_v1 = []
            pos = p
            for _ in range(4):
                if pos + 4 <= len(data):
                    v = struct.unpack('>I', data[pos:pos+4])[0]
                    vals_v1.append(v)
                    pos += 4
                else:
                    vals_v1.append(-1)
            
            print(f"\n  [{label}] offset={off}:")
            print(f"    V2.0: kb={vals_v2[0]} entries={vals_v2[1]} idxDecomp={vals_v2[2]} idxComp={vals_v2[3]} kbLen={vals_v2[4]} cs={vals_v2[5]}")
            print(f"    V1.2: kb={vals_v1[0]} entries={vals_v1[1]} idxComp={vals_v1[2]} kbLen={vals_v1[3]}")
    
    # Also search for </Dictionary> to find actual end of header
    closing_tag = b'</Dictionary>'
    idx = data.find(closing_tag)
    if idx >= 0:
        actual_header_end = idx + len(closing_tag)
        print(f"\n  Found </Dictionary> at byte offset {idx}, actual header ends at ~{actual_header_end}")
        print(f"  Difference from calculated: {actual_header_end - hdr_end} bytes")
    
    print("\n")
