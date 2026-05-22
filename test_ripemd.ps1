Add-Type -TypeDefinition @"
using System;
using System.Text;

public class RipeMD128Test {
    static uint f(int x, uint y, uint z) {
        switch(x) {
            case 0: return (y & z) | (~y & ~z);
            case 1: return (y & z) | (y & ~z);
            case 2: return y ^ z ^ (y & ~z);
            case 3: return y ^ (z | ~y);
            default: return (z & y) | (~z & ~y);
        }
    }
    
    static uint RL(int j) {
        int[,] r = {{0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,
                     7,4,13,1,10,6,15,3,12,0,9,5,2,14,11,8,
                     3,10,14,4,9,15,8,1,2,7,0,6,13,11,5,12,
                     1,9,11,10,0,8,12,4,13,3,7,15,14,5,6,2,
                     4,0,5,9,7,12,2,10,14,1,3,8,11,6,15,13}};
        return (uint)r[j/16, j%16];
    }
    
    static int SL(int j) {
        int[,] s = {{11,14,15,12,5,8,7,9,11,13,14,15,6,7,9,8,
                     7,6,8,13,11,9,7,15,7,12,15,9,11,7,13,13,
                     6,5,7,14,9,13,12,5,14,13,13,7,5,15,5,8,
                     11,12,14,15,14,15,9,8,9,14,5,6,8,6,5,12,
                     9,15,5,11,6,8,13,12,5,12,13,14,11,8,5,6}};
        return s[j/16, j%16];
    }
    
    public static string Digest(string input) {
        byte[] data = Encoding.UTF8.GetBytes(input);
        uint h0=0x67452301,h1=0xEFCDAB89,h2=0x98BADCFE,h3=0x10325476;
        
        long msgLen = (long)data.Length * 8L;
        int padLen = (64 - ((data.Length + 9) % 64)) % 64 + 9;
        byte[] padded = new byte[data.Length + padLen];
        Array.Copy(data, padded, data.Length);
        padded[data.Length] = 0x80;
        for (int i=0;i<8;i++) padded[padded.Length-8+i] = (byte)(msgLen >> (i*8));
        
        for (int off=0;off<padded.Length;off+=64) {
            uint al=h0,bl=h1,cl=h2,dl=h3,ar=h0,br=h1,cr=h2,dr=h3;
            
            for (int j=0;j<80;j++) {
                uint x=BitConverter.ToUInt32(padded, off+(int)(RL(j)*4));
                uint t=(al+f(j%5,bl,cl)+x+KL(j));
                al=(t<<SL(j))|(t>>(32-SL(j)));
                {var tmp=dl; dl=cl; cl=bl; bl=t;}
                
                x=BitConverter.ToUInt32(padded, off+(int)(RR(j)*4));
                t=(ar+f(4-j%5,br,cr,dr)+x+KR(j));
                ar=(t<<SR(j))|(t>>(32-SR(j)));
                {var tmp2=dr; dr=br; br=t;}
            }
            
            uint tt=h1+cl+dr; h0=h2+al+ar; h1=h3+bl+br; h2=h0+cl+dr; h3=tt;
        }
        
        byte[] result=new byte[16];
        BitConverter.GetBytes(h0).CopyTo(result,0);
        BitConverter.GetBytes(h1).CopyTo(result,4);
        BitConverter.GetBytes(h2).CopyTo(result,8);
        BitConverter.GetBytes(h3).CopyTo(result,12);
        return BitConverter.ToString(result).Replace("-","");
    }
    
    static uint KL(int j) { uint[] k={0,0x5A827999,0x6ED9EBA1,0x8F1BBCDC,0xA953FD4E}; return k[j/16]; }
    static uint KR(int j) { uint[] k={0x50A28BE6,0x5C4DD124,0x6D703EF3,0x7A6D76E9,0}; return k[j/16]; }
    
    static uint RR(int j) {
        int[,] r={{5,14,7,0,9,2,11,4,13,6,15,8,1,10,3,12,
                    6,11,3,7,0,13,5,10,14,15,8,12,4,9,1,2,
                    15,5,1,3,7,14,6,9,11,8,12,2,10,0,4,13,
                    8,6,4,1,3,11,15,0,5,12,2,13,9,7,10,14,
                    12,15,10,4,1,5,8,7,6,2,13,14,0,3,9,11}};
        return (uint)r[j/16,j%16];
    }
    static int SR(int j) {
        int[,] s={{8,9,9,11,13,15,15,5,7,7,8,11,14,14,12,6,
                    9,13,15,7,12,8,9,11,7,7,12,7,6,15,13,11,
                    9,7,15,11,8,6,6,14,12,13,5,14,13,13,7,5,
                    15,5,8,11,14,14,6,14,6,9,12,9,12,5,15,8,
                    8,5,12,9,12,5,14,6,8,13,6,5,15,13,11,11}};
        return s[j/16,j%16];
    }
}
"@

$outPath = "d:\workspace\Gdict\ripemd_test_result.txt"
$sb = New-Object System.Text.StringBuilder
[void]$sb.AppendLine("RIPEMD-128 Test Vectors:")
[void]$sb.AppendLine("")

$tests = @(
    @("", "CDF26213A55DC47A4D6B1A5E3E8F4E2C"),
    @("a", "86BE7AFBFA6179789D2BC0372DA874CA"),
    @("abc", "C14A12199C66E4BA84636B0F881627FC"),
    @("message digest", "9E327B3D6E523062AFC113AD40FE9DAE")
)

foreach ($t in $tests) {
    $input = $t[0]
    $expected = $t[1]
    $result = [RipeMD128Test]::Digest($input)
    $status = if ($result.ToUpper() -eq $expected) { "PASS" } else { "FAIL" }
    [void]$sb.AppendLine("Input: '$input'")
    [void]$sb.AppendLine("  Expected: $expected")
    [void]$sb.AppendLine("  Got:      $($result.ToUpper())")
    [void]$sb.AppendLine("  Status:   $status")
    [void]$sb.AppendLine("")
}

[System.IO.File]::WriteAllText($outPath, $sb.ToString(), [System.Text.Encoding]::UTF8)
Write-Host "Done! Output written to $outPath"
