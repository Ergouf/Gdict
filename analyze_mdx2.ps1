Add-Type -TypeDefinition @"
using System;
using System.IO;
using System.Text;

public class MdxAnalyzer2 {
    public static string Analyze(string filePath) {
        var sb = new StringBuilder();
        if (!File.Exists(filePath)) return "NOT FOUND: " + filePath;
        
        byte[] data = File.ReadAllBytes(filePath);
        string name = Path.GetFileName(filePath);
        sb.AppendLine("=== " + name + " (" + data.Length + " bytes) ===");
        
        int headerLenBE = (data[0] << 24) | (data[1] << 16) | (data[2] << 8) | data[3];
        sb.AppendLine("First4: " + data[0].ToString("X2") + " " + data[1].ToString("X2") + " " 
            + data[2].ToString("X2") + " " + data[3].ToString("X2") + " -> headerLen=" + headerLenBE);
        
        int hdrEnd = 4 + headerLenBE + 4;
        sb.AppendLine("Header ends at offset: " + hdrEnd + " (file size: " + data.Length + ")");
        
        string headerXml = Encoding.Unicode.GetString(data, 4, headerLenBE);
        
        // Extract key attributes
        string engineVer = ExtractAttr(headerXml, "GeneratedByEngineVersion");
        string enc = ExtractAttr(headerXml, "Encrypted");
        string encoding = ExtractAttr(headerXml, "Encoding");
        string title = ExtractAttr(headerXml, "Title");
        string regBy = ExtractAttr(headerXml, "RegisterBy");
        string compact = ExtractAttr(headerXml, "Compact");
        
        sb.AppendLine("--- Key Attributes ---");
        sb.AppendLine("  Title:           " + (title ?? "(null)"));
        sb.AppendLine("  EngineVersion:   " + (engineVer ?? "(null)"));
        sb.AppendLine("  Encrypted:       " + (enc ?? "(null)"));
        sb.AppendLine("  Encoding:        " + (encoding ?? "(null)"));
        sb.AppendLine("  RegisterBy:      " + (regBy ?? "(null)"));
        sb.AppendLine("  Compact:         " + (compact ?? "(null)"));
        
        // Parse keyword section
        if (hdrEnd + 48 <= data.Length) {
            long kb = ReadInt64BE(data, hdrEnd);
            long te = ReadInt64BE(data, hdrEnd + 8);
            long idl = ReadInt64BE(data, hdrEnd + 16);
            long icl = ReadInt64BE(data, hdrEnd + 24);
            long kbl = ReadInt64BE(data, hdrEnd + 32);
            
            sb.AppendLine("");
            sb.AppendLine("--- Keyword Section Header (V2.0) ---");
            sb.AppendLine("  numKeyBlocks     = " + kb);
            sb.AppendLine("  totalEntries      = " + te);
            sb.AppendLine("  keyIndexDecompLen = " + idl);
            sb.AppendLine("  keyIndexCompLen   = " + icl);
            sb.AppendLine("  keyBlocksLen      = " + kbl);
            
            // Show first bytes of compressed key index
            int kwStart = hdrEnd + 44; // after 5*8 + 4 checksum
            if (kwStart + 16 <= data.Length) {
                sb.AppendLine("");
                sb.AppendLine("--- Compressed Key Index (first 16 bytes at " + kwStart + ") ---");
                for (int i = kwStart; i < Math.Min(kwStart + 16, data.Length); i++) {
                    sb.Append(data[i].ToString("X2") + " ");
                }
                
                int compType = (data[kwStart]) | (data[kwStart+1] << 8) | (data[kwStart+2] << 16) | (data[kwStart+3] << 24);
                sb.AppendLine("");
                sb.AppendLine("  compType = " + compType + " (0=" + (compType==0?"none":(compType==1?"LZO":(compType==2?"zlib":"unknown"))) + ")");
            }
        }
        
        return sb.ToString();
    }
    
    static string ExtractAttr(string xml, string attrName) {
        var pattern = attrName + "=\"([^\"]*)\"";
        var m = System.Text.RegularExpressions.Regex.Match(xml, pattern, System.Text.RegularExpressions.RegexOptions.IgnoreCase);
        return m.Success ? m.Groups[1].Value : null;
    }
    
    static long ReadInt64BE(byte[] d, int off) {
        return ((long)d[off] << 56) | ((long)d[off+1] << 48) | ((long)d[off+2] << 40) | ((long)d[off+3] << 32)
             | ((long)d[off+4] << 24) | ((long)d[off+5] << 16) | ((long)d[off+6] << 8) | d[off+7];
    }
}
"@

$outPath = "d:\workspace\Gdict\mdx_analysis2.txt"
$sb = New-Object System.Text.StringBuilder

$files = @(
    "d:\workspace\Gdict\[英-英] 柯林斯第三版 Collins 3rd （非wh_cxh、ldlcau版本）collins 3.mdx",
    "d:\workspace\Gdict\Cambridge_English_Pronouncing_Dictionary_18th.mdx",
    "d:\workspace\Gdict\Collins COBUILD Advanced English Dictionary Online.mdx"
)

foreach ($f in $files) {
    $result = [MdxAnalyzer2]::Analyze($f)
    [void]$sb.AppendLine($result)
    [void]$sb.AppendLine([string]::new('=', 70))
    [void]$sb.AppendLine()
}

[System.IO.File]::WriteAllText($outPath, $sb.ToString(), [System.Text.Encoding]::UTF8)
Write-Host "Done! Output written to $outPath"
