Add-Type -TypeDefinition @"
using System;
using System.IO;
using System.Text;

public class MdxAnalyzer {
    public static string Analyze(string filePath) {
        var sb = new StringBuilder();
        if (!File.Exists(filePath)) return "NOT FOUND: " + filePath;
        
        byte[] data = File.ReadAllBytes(filePath);
        string name = Path.GetFileName(filePath);
        sb.AppendLine("=== " + name + " (" + data.Length + " bytes) ===");
        
        // Header length
        int headerLenBE = (data[0] << 24) | (data[1] << 16) | (data[2] << 8) | data[3];
        sb.AppendLine("First4: " + data[0].ToString("X2") + " " + data[1].ToString("X2") + " " 
            + data[2].ToString("X2") + " " + data[3].ToString("X2") + " -> headerLen=" + headerLenBE);
        
        int hdrEnd = 4 + headerLenBE + 4;
        sb.AppendLine("Header ends at offset: " + hdrEnd + " (file size: " + data.Length + ")");
        
        // Header XML
        string headerXml = Encoding.Unicode.GetString(data, 4, headerLenBE);
        sb.AppendLine("--- Header XML ---");
        sb.AppendLine(headerXml);
        
        bool hasClosing = headerXml.Contains("</Dictionary>");
        sb.AppendLine("Has </Dictionary>: " + hasClosing);
        sb.AppendLine("Last 50 chars: ..." + headerXml.Substring(Math.Max(0, headerXml.Length - 50)));
        
        // Find actual end of Dictionary tag
        byte[] closingTag = Encoding.Unicode.GetBytes("</Dictionary>");
        int closeIdx = -1;
        for (int i = 0; i < data.Length - closingTag.Length; i++) {
            bool found = true;
            for (int j = 0; j < closingTag.Length; j++) {
                if (data[i+j] != closingTag[j]) { found = false; break; }
            }
            if (found) { closeIdx = i; break; }
        }
        if (closeIdx >= 0) {
            int actualEnd = closeIdx + closingTag.Length;
            sb.AppendLine("Found </Dictionary> at UTF16-byte-offset: " + closeIdx + ", actual end: " + actualEnd);
            sb.AppendLine("Diff from calculated: " + (actualEnd - hdrEnd));
            
            // Try parsing keyword section at actual end
            int actualKwStart = actualEnd + 4; // +4 for checksum
            if (actualKwStart + 48 <= data.Length) {
                long kb = ReadInt64BE(data, actualKwStart);
                long te = ReadInt64BE(data, actualKwStart + 8);
                long idl = ReadInt64BE(data, actualKwStart + 16);
                long icl = ReadInt64BE(data, actualKwStart + 24);
                long kbl = ReadInt64BE(data, actualKwStart + 32);
                sb.AppendLine("");
                sb.AppendLine("V2.0 Keyword Section at actual end (" + actualKwStart + "):");
                sb.AppendLine("  numKeyBlocks     = " + kb);
                sb.AppendLine("  totalEntries      = " + te);
                sb.AppendLine("  keyIndexDecompLen = " + idl);
                sb.AppendLine("  keyIndexCompLen   = " + icl);
                sb.AppendLine("  keyBlocksLen      = " + kbl);
                
                // Also show hex
                sb.AppendLine("");
                sb.AppendLine("Hex bytes at " + actualKwStart + ":");
                for (int i = actualKwStart; i < Math.Min(actualKwStart + 48, data.Length); i += 16) {
                    string hex = "";
                    string ascii = "";
                    for (int j = i; j < Math.Min(i+16, data.Length); j++) {
                        hex += data[j].ToString("X2") + " ";
                        ascii += (data[j] >= 32 && data[j] < 127) ? ((char)data[j]).ToString() : ".";
                    }
                    sb.AppendLine("  " + i.ToString().PadLeft(6) + ": " + hex.PadRight(49) + " " + ascii);
                }
            }
        }
        
        // Hex at calculated header end
        if (hdrEnd + 48 <= data.Length) {
            sb.AppendLine("");
            sb.AppendLine("Hex at calculated header end (" + hdrEnd + "):");
            for (int i = hdrEnd; i < Math.Min(hdrEnd + 48, data.Length); i += 16) {
                string hex = "";
                string ascii = "";
                for (int j = i; j < Math.Min(i+16, data.Length); j++) {
                    hex += data[j].ToString("X2") + " ";
                    ascii += (data[j] >= 32 && data[j] < 127) ? ((char)data[j]).ToString() : ".";
                }
                sb.AppendLine("  " + i.ToString().PadLeft(6) + ": " + hex.PadRight(49) + " " + ascii);
            }
        }
        
        return sb.ToString();
    }
    
    static long ReadInt64BE(byte[] d, int off) {
        return ((long)d[off] << 56) | ((long)d[off+1] << 48) | ((long)d[off+2] << 40) | ((long)d[off+3] << 32)
             | ((long)d[off+4] << 24) | ((long)d[off+5] << 16) | ((long)d[off+6] << 8) | d[off+7];
    }
}
"@

$outPath = "d:\workspace\Gdict\mdx_analysis.txt"
$sb = New-Object System.Text.StringBuilder

$f1 = "d:\workspace\Gdict\Cambridge_English_Pronouncing_Dictionary_18th.mdx"
$f2 = "d:\workspace\Gdict\Collins COBUILD Advanced English Dictionary Online.mdx"

foreach ($f in @($f1, $f2)) {
    $result = [MdxAnalyzer]::Analyze($f)
    [void]$sb.AppendLine($result)
    [void]$sb.AppendLine()
    [void]$sb.AppendLine([string]::new('=', 70))
    [void]$sb.AppendLine()
}

[System.IO.File]::WriteAllText($outPath, $sb.ToString(), [System.Text.Encoding]::UTF8)
Write-Host "Done! Output written to $outPath"
