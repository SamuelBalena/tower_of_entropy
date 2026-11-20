param([int]$ClickX=-1,[int]$ClickY=-1,[string]$Text='', [int]$Key=0,[string]$Capture='')
# Scoped diagnostic interaction: only the Forge 1.20.1 test window, never desktop capture.
$ErrorActionPreference='Stop'
Add-Type -AssemblyName System.Drawing
Add-Type @'
using System; using System.Runtime.InteropServices;
public class ToeDevWindow {
 [DllImport("user32.dll")] public static extern bool PrintWindow(IntPtr h,IntPtr dc,uint f);
 [DllImport("user32.dll")] public static extern bool GetWindowRect(IntPtr h,out RECT r);
 [DllImport("user32.dll")] public static extern bool PostMessage(IntPtr h,int m,IntPtr w,IntPtr l);
 [StructLayout(LayoutKind.Sequential)] public struct RECT { public int L,T,R,B; }
}
'@
$p=Get-Process java | Where-Object {$_.MainWindowTitle.StartsWith('Minecraft* Forge 1.20.1')} | Select-Object -First 1
if(!$p){throw 'Expected Forge test window not found'}
$h=$p.MainWindowHandle
if($ClickX -ge 0){
 $point=[IntPtr](($ClickY -shl 16) -bor ($ClickX -band 65535))
 [void][ToeDevWindow]::PostMessage($h,0x200,[IntPtr]::Zero,$point)
 [void][ToeDevWindow]::PostMessage($h,0x201,[IntPtr]1,$point)
 [void][ToeDevWindow]::PostMessage($h,0x202,[IntPtr]::Zero,$point)
}
foreach($ch in $Text.ToCharArray()){[void][ToeDevWindow]::PostMessage($h,0x102,[IntPtr][int]$ch,[IntPtr]::Zero)}
if($Key -gt 0){
 [void][ToeDevWindow]::PostMessage($h,0x100,[IntPtr]$Key,[IntPtr]::Zero)
 Start-Sleep -Milliseconds 80
 [void][ToeDevWindow]::PostMessage($h,0x101,[IntPtr]$Key,[IntPtr]::Zero)
}
if($Capture){
 Start-Sleep -Milliseconds 400
 $r=New-Object ToeDevWindow+RECT
 [void][ToeDevWindow]::GetWindowRect($h,[ref]$r)
 $b=New-Object Drawing.Bitmap ($r.R-$r.L),($r.B-$r.T)
 $g=[Drawing.Graphics]::FromImage($b);$dc=$g.GetHdc()
 try{if(![ToeDevWindow]::PrintWindow($h,$dc,2)){throw 'Window capture failed'}}finally{$g.ReleaseHdc($dc)}
 $b.Save([IO.Path]::GetFullPath($Capture));$g.Dispose();$b.Dispose()
}
