// JUGCRAFT direct-distribution updater. OpenAI Codex (GPT-6), owner requested.
// No launcher database writes, downloaded scripts, credentials, or privilege elevation.
using System;
using System.Collections.Generic;
using System.Diagnostics;
using System.Drawing;
using System.IO;
using System.Linq;
using System.Management;
using System.Net;
using System.Security.Cryptography;
using System.Text;
using System.Text.RegularExpressions;
using System.Threading;
using System.Threading.Tasks;
using System.Web.Script.Serialization;
using System.Windows.Forms;
using Microsoft.Win32;

namespace Jugcraft {
public class FileSpec { public string path, sha512, url, policy; public long size; }
public class PackSpec { public string url, sha512; public long size; }
public class Manifest {
    public int schemaVersion, updaterVersion, java;
    public string releaseVersion, sourceCommit, minecraft, fabricLoader, serverAddress;
    public FileSpec[] files;
    public PackSpec pack;
}
public class Channel { public int schemaVersion; public string releaseVersion, manifestUrl, manifestSha512; }
public class Binding { public string profile; public bool importPending; }
public class Change { public string path; public bool existed; }
public class Journal { public Change[] changes; public Manifest previous; }

public static class Engine {
    public const string ChannelUrl = "https://github.com/jimbozoomer-byte/jugcraft/releases/download/player-channel/stable.json";
    public const string ReleasePrefix = "https://github.com/jimbozoomer-byte/jugcraft/releases/download/";
    public static readonly JavaScriptSerializer Json = new JavaScriptSerializer { MaxJsonLength = 2 * 1024 * 1024 };
    public static string AppDir = Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData), "JUGCRAFT");
    public static Action<string> Log = delegate {};
    public static void Require(bool ok, string message) { if (!ok) throw new InvalidOperationException(message); }
    public static string Hash(byte[] data) { using (var h = SHA512.Create()) return BitConverter.ToString(h.ComputeHash(data)).Replace("-", "").ToLowerInvariant(); }
    public static string HashFile(string path) { using (var h = SHA512.Create()) using (var s = File.OpenRead(path)) return BitConverter.ToString(h.ComputeHash(s)).Replace("-", "").ToLowerInvariant(); }
    public static bool Digest(string s) { return s != null && Regex.IsMatch(s, "^[0-9a-f]{128}$"); }
    public static bool Version(string s) { return s != null && Regex.IsMatch(s, "^[A-Za-z0-9][A-Za-z0-9._+\\-]{0,79}$"); }
    public static string NormalPath(string path) {
        if(path.StartsWith(@"\\?\UNC\",StringComparison.OrdinalIgnoreCase)) path=@"\\"+path.Substring(8);
        else if(path.StartsWith(@"\\?\",StringComparison.Ordinal)) path=path.Substring(4);
        return Path.GetFullPath(path).TrimEnd(Path.DirectorySeparatorChar);
    }
    public static void NoLinks(string path) {
        for (var p = new DirectoryInfo(Path.GetFullPath(path)); p != null; p = p.Parent)
            if (p.Exists) Require((p.Attributes & FileAttributes.ReparsePoint) == 0, "Linked/junction directories are not supported: " + p.FullName);
        if (File.Exists(path)) Require((File.GetAttributes(path) & FileAttributes.ReparsePoint) == 0, "Linked file is not supported.");
    }
    public static string Child(string root, string relative) {
        Require(relative != null && Regex.IsMatch(relative, "^mods/[A-Za-z0-9][A-Za-z0-9._+\\-]*\\.jar$"), "Invalid managed mod path.");
        var name = Path.GetFileNameWithoutExtension(relative);
        Require(!Regex.IsMatch(name, "^(CON|PRN|AUX|NUL|COM[1-9]|LPT[1-9])($|\\.)", RegexOptions.IgnoreCase), "Reserved file name.");
        var result = Path.GetFullPath(Path.Combine(root, relative.Replace('/', Path.DirectorySeparatorChar)));
        Require(result.StartsWith(Path.GetFullPath(root).TrimEnd(Path.DirectorySeparatorChar) + Path.DirectorySeparatorChar, StringComparison.OrdinalIgnoreCase), "Path escapes profile.");
        NoLinks(result); return result;
    }
    public static void Url(string value, bool library) {
        Uri u; Require(Uri.TryCreate(value, UriKind.Absolute, out u) && u.Scheme == "https" && u.IsDefaultPort && u.UserInfo == "" && u.Fragment == "" && u.Query == "", "Invalid download URL.");
        bool pinnedGuiLib = library && Regex.IsMatch(value, "^https://raw\\.githubusercontent\\.com/SkyblockOverhaul/maven/[0-9a-f]{40}/net/sbo/[A-Za-z0-9._/+\\-]+\\.jar$");
        Require(value.StartsWith(ReleasePrefix, StringComparison.Ordinal) || (library && u.Host == "cdn.modrinth.com" && u.AbsolutePath.StartsWith("/data/")) || pinnedGuiLib, "Download source is not allowed.");
    }
    public static void Validate(Manifest m) {
        Require(m != null && m.schemaVersion == 1 && m.updaterVersion == 1 && Version(m.releaseVersion), "Unsupported release; download the current installer.");
        Require(m.java == 25 && m.minecraft == "26.3" && m.fabricLoader == "0.19.5", "Platform changed. This updater will not silently change Minecraft or Fabric.");
        Require(m.files != null && m.files.Length > 0 && m.files.Length < 200, "Invalid mod inventory.");
        var names = new HashSet<string>(StringComparer.OrdinalIgnoreCase);
        foreach (var f in m.files) {
            Require(f != null && names.Add(f.path) && f.policy == "managed" && Digest(f.sha512) && f.size > 0 && f.size <= 1024L*1024*1024, "Invalid managed file.");
            Child(Path.GetTempPath(), f.path); Url(f.url, true);
            if (f.url.StartsWith(ReleasePrefix)) Require(f.url.StartsWith(ReleasePrefix + "pack-" + m.releaseVersion + "/"), "Artifact version mismatch.");
        }
        Require(names.Contains("mods/jugcraft.jar"), "Release has no Jugcraft mod.");
        Require(m.pack != null && Digest(m.pack.sha512) && m.pack.size > 0 && m.pack.size <= 2L*1024*1024*1024, "Invalid initial pack.");
        Url(m.pack.url, false);
        Require(m.pack.url.StartsWith(ReleasePrefix + "pack-" + m.releaseVersion + "/") && m.pack.url.EndsWith(".mrpack"), "Invalid initial pack URL.");
    }
    public static byte[] Download(string url, long limit) {
        Url(url, true);
        for (int redirect=0; redirect<6; redirect++) {
        var request = (HttpWebRequest)WebRequest.Create(url);
        request.AllowAutoRedirect=false;
        request.UserAgent = "Jugcraft-Updater/1"; request.Timeout = 30000; request.ReadWriteTimeout = 30000;
        using (var response = (HttpWebResponse)request.GetResponse()) {
            int status=(int)response.StatusCode;
            if(status>=300 && status<400) {
                var next=new Uri(new Uri(url),response.Headers["Location"]);
                Require(next.Scheme=="https" && next.IsDefaultPort && next.UserInfo=="" && new[]{"github.com","release-assets.githubusercontent.com","objects.githubusercontent.com","cdn.modrinth.com","raw.githubusercontent.com"}.Contains(next.Host),"Download redirect rejected.");
                url=next.AbsoluteUri; continue;
            }
            Require(response.ContentLength <= limit, "Download larger than expected.");
            using (var input = response.GetResponseStream()) using (var output = new MemoryStream()) {
                var buffer = new byte[65536]; int count;
                while ((count = input.Read(buffer, 0, buffer.Length)) > 0) { Require(output.Length + count <= limit, "Download larger than expected."); output.Write(buffer, 0, count); }
                return output.ToArray();
            }
        }}
        throw new InvalidOperationException("Too many download redirects.");
    }
    public static Manifest Latest() {
        Log("Checking the owner-approved JUGCRAFT release...");
        var c = Json.Deserialize<Channel>(Encoding.UTF8.GetString(Download(ChannelUrl, 32768)));
        Require(c != null && c.schemaVersion == 1 && Version(c.releaseVersion) && Digest(c.manifestSha512), "Invalid update channel.");
        Require(c.manifestUrl == ReleasePrefix + "pack-" + c.releaseVersion + "/player-update.json", "Invalid manifest address.");
        var bytes = Download(c.manifestUrl, 2*1024*1024);
        Require(Hash(bytes) == c.manifestSha512, "Release manifest checksum mismatch.");
        var m = Json.Deserialize<Manifest>(Encoding.UTF8.GetString(bytes)); Validate(m);
        Require(m.releaseVersion == c.releaseVersion, "Channel version mismatch."); return m;
    }
    public static void WriteJson(string path, object value) {
        NoLinks(path); Directory.CreateDirectory(Path.GetDirectoryName(path));
        string temp = path + ".new"; NoLinks(temp);
        File.WriteAllText(temp, Json.Serialize(value), new UTF8Encoding(false));
        if (File.Exists(path)) File.Replace(temp, path, null); else File.Move(temp, path);
    }
    public static T Read<T>(string path) { NoLinks(path); return Json.Deserialize<T>(File.ReadAllText(path)); }
    public static string Work(string profile) { var p = Path.Combine(profile, ".jugcraft-update"); NoLinks(p); return p; }
    public static bool Matches(string path, FileSpec f) { return File.Exists(path) && new FileInfo(path).Length == f.size && HashFile(path) == f.sha512; }
    public static Manifest Installed(string profile) { return Read<Manifest>(Path.Combine(Work(profile), "installed.json")); }
    public static void Bind(string profile, Manifest m) {
        Validate(m); NoLinks(profile);
        Require(Directory.Exists(profile), "Choose the imported JUGCRAFT instance folder.");
        string work = Work(profile); Directory.CreateDirectory(work);
        using(var gate=new FileStream(Path.Combine(work,"update.lock"),FileMode.OpenOrCreate,FileAccess.ReadWrite,FileShare.None)) {
            Recover(profile);
            if(File.Exists(Path.Combine(work,"installed.json"))) Validate(Installed(profile));
            else {
                foreach (var f in m.files) Require(Matches(Child(profile, f.path), f), "This instance does not match the downloaded release: " + f.path + ". Finish importing the pack, then select its folder.");
                WriteJson(Path.Combine(work, "installed.json"), m);
            }
        }
        WriteJson(Path.Combine(AppDir, "binding.json"), new Binding { profile = NormalPath(profile), importPending = false });
    }
    public static void EnsureStopped(string profile) {
        // Fail closed if process inspection is unavailable. Read only Java command lines,
        // never print them (Minecraft authentication arguments may be present).
        using (var search = new ManagementObjectSearcher("SELECT Name,CommandLine FROM Win32_Process WHERE Name='java.exe' OR Name='javaw.exe'"))
        using (var rows = search.Get()) foreach (ManagementObject row in rows) {
            string cmd = row["CommandLine"] as string;
            Require(cmd != null, "Cannot confirm Minecraft is closed. Close Minecraft before updating.");
            Require(cmd.IndexOf(profile, StringComparison.OrdinalIgnoreCase) < 0 && cmd.IndexOf(profile.Replace('\\','/'), StringComparison.OrdinalIgnoreCase) < 0, "Close this Minecraft instance before updating.");
        }
    }
    public static void Recover(string profile) {
        var work = Work(profile); var journalPath = Path.Combine(work, "transaction.json");
        if (!File.Exists(journalPath)) return;
        var j = Read<Journal>(journalPath); Validate(j.previous);
        Require(j.changes != null && j.changes.Length <= 400, "Invalid recovery record.");
        foreach (var c in j.changes) {
            var target = Child(profile, c.path); var backup = Child(Path.Combine(work, "backup"), c.path);
            if (c.existed) { var original=j.previous.files.SingleOrDefault(f=>f.path==c.path); Require(original!=null && Matches(backup,original), "Recovery backup missing or corrupt; do not launch. " + c.path); File.Copy(backup, target, true); }
            else if (File.Exists(target)) File.Delete(target);
        }
        WriteJson(Path.Combine(work, "installed.json"), j.previous);
        File.Delete(journalPath); Log("Recovered the previous complete installation.");
    }
    public static List<Change> Plan(string profile, Manifest previous, Manifest next) {
        Validate(previous); Validate(next);
        Require(previous.minecraft == next.minecraft && previous.fabricLoader == next.fabricLoader, "Platform migration requires a new reviewed installer.");
        var old = previous.files.ToDictionary(f=>f.path, StringComparer.OrdinalIgnoreCase);
        var wanted = next.files.ToDictionary(f=>f.path, StringComparer.OrdinalIgnoreCase);
        var changes = new List<Change>();
        foreach (var f in next.files) {
            string p = Child(profile, f.path); if (Matches(p, f)) continue;
            if (File.Exists(p)) Require(old.ContainsKey(f.path) && Matches(p, old[f.path]), "Managed file was edited: " + f.path + ". Restore it or ask for help; your file was kept.");
            changes.Add(new Change { path = f.path, existed = File.Exists(p) });
        }
        foreach (var f in previous.files.Where(f=>!wanted.ContainsKey(f.path))) {
            string p = Child(profile, f.path); if (!File.Exists(p)) continue;
            Require(Matches(p, f), "Obsolete managed file was edited: " + f.path + ". Your file was kept.");
            changes.Add(new Change { path=f.path, existed=true });
        }
        return changes;
    }
    // Injected fetch/fault callbacks are used only by the offline test harness, never by channel data.
    public static void Apply(string profile, Manifest next, Func<FileSpec,byte[]> fetch, Action<int> fault) {
        Recover(profile); var previous = Installed(profile); var changes = Plan(profile, previous, next);
        string work = Work(profile), stage = Path.Combine(work, "stage"), backup = Path.Combine(work, "backup");
        var wanted = next.files.ToDictionary(f=>f.path, StringComparer.OrdinalIgnoreCase);
        // All downloads and hashes succeed before touching any active mod.
        foreach (var c in changes.Where(c=>wanted.ContainsKey(c.path))) {
            var f = wanted[c.path]; Log("Downloading " + f.path);
            var bytes = fetch(f); Require(bytes.LongLength == f.size && Hash(bytes) == f.sha512, "Download verification failed: " + f.path);
            string p = Child(stage, c.path); Directory.CreateDirectory(Path.GetDirectoryName(p)); File.WriteAllBytes(p, bytes);
        }
        // Detect changes made during download, before any commit.
        var recheck = Plan(profile, previous, next);
        Require(Json.Serialize(changes) == Json.Serialize(recheck), "Files changed during download. Retry with Minecraft closed.");
        foreach (var c in changes.Where(c=>c.existed)) {
            string p = Child(backup, c.path); Directory.CreateDirectory(Path.GetDirectoryName(p)); File.Copy(Child(profile, c.path), p, true);
        }
        WriteJson(Path.Combine(work, "transaction.json"), new Journal { changes=changes.ToArray(), previous=previous });
        try {
            int count=0;
            foreach (var c in changes) {
                string p = Child(profile, c.path);
                if (wanted.ContainsKey(c.path)) { Directory.CreateDirectory(Path.GetDirectoryName(p)); File.Copy(Child(stage, c.path), p, true); }
                else File.Delete(p);
                fault(++count);
            }
            WriteJson(Path.Combine(work, "installed.json"), next);
            File.Delete(Path.Combine(work, "transaction.json"));
        } catch { Recover(profile); throw; }
        // Clear only named staged files. Keep one per-path rollback copy; no recursive deletes.
        foreach (var c in changes.Where(c=>wanted.ContainsKey(c.path))) File.Delete(Child(stage, c.path));
        Log("JUGCRAFT " + next.releaseVersion + " is ready. Settings and saves preserved.");
    }
}

public class SetupForm : Form {
    TextBox output = new TextBox { Multiline=true, ReadOnly=true, ScrollBars=ScrollBars.Vertical, Dock=DockStyle.Fill, BackColor=Color.FromArgb(29,34,40), ForeColor=Color.White, Font=new Font("Segoe UI",10) };
    FlowLayoutPanel buttons = new FlowLayoutPanel { Dock=DockStyle.Bottom, Height=100, Padding=new Padding(8) };
    public SetupForm() {
        Text="JUGCRAFT • Install and update"; Size=new Size(740,480); MinimumSize=Size; StartPosition=FormStartPosition.CenterScreen;
        Controls.Add(output); Controls.Add(buttons);
        output.BorderStyle=BorderStyle.None; output.TabStop=false;
        var heading=new Label {Text="JUGCRAFT",Dock=DockStyle.Top,Height=65,Padding=new Padding(12,15,0,0),BackColor=Color.FromArgb(21,26,31),ForeColor=Color.FromArgb(101,220,165),Font=new Font("Segoe UI",20,FontStyle.Bold)};
        Controls.Add(heading);
        Add("1. Install once", Install); Add("2. Select instance folder", Attach); Add("3. Copy launch hook", Hook); Add("Check for update", Check);
        Engine.Log = s=> { if (!IsDisposed) Invoke((Action)(()=>output.AppendText(s+Environment.NewLine))); };
        output.Text="JUGCRAFT\r\nOne instance. Your settings stay yours.\r\n\r\nFirst time: install once, then select its folder using Modrinth's Open folder button.\r\nAlready installed? Select that instance instead.\r\nFinally copy the hook into this instance's Settings > Launch hooks > Pre-launch.\r\nDo not set a global hook or replace a hook you already use.\r\n\r\nPlay through Modrinth after setup. No public pack listing is needed.\r\n";
        if (File.Exists(Path.Combine(Engine.AppDir,"binding.json"))) output.AppendText("Existing setup found. Installing again will not create another instance.\r\n");
        Shown+=(s,e)=> { output.SelectionLength=0; ActiveControl=buttons.Controls[0]; };
    }
    void Add(string label, Action action) { var b=new Button { Text=label, AutoSize=true, Height=34 }; b.Click+=(s,e)=> { try { action(); } catch(Exception ex) { MessageBox.Show(this,ex.Message,"JUGCRAFT",MessageBoxButtons.OK,MessageBoxIcon.Error); } }; buttons.Controls.Add(b); }
    void Background(Action action) { buttons.Enabled=false; Task.Run(()=> { try { action(); } catch(Exception e) { Engine.Log(e.Message); } finally { if(!IsDisposed) Invoke((Action)(()=>buttons.Enabled=true)); } }); }
    Binding Binding() { var p=Path.Combine(Engine.AppDir,"binding.json"); return File.Exists(p)?Engine.Read<Binding>(p):new Binding(); }
    void Install() {
        var binding=Binding();
        if (!String.IsNullOrEmpty(binding.profile)) { Check(); return; }
        if (binding.importPending) { MessageBox.Show(this,"An import has already been opened. Finish it in Modrinth, then select that instance folder. No second import will be created.","JUGCRAFT"); return; }
        Background(()=> {
            var m=Engine.Latest(); Engine.Log("Downloading initial pack. Updates later download only changed mods.");
            var bytes=Engine.Download(m.pack.url,m.pack.size); Engine.Require(bytes.LongLength==m.pack.size && Engine.Hash(bytes)==m.pack.sha512,"Initial pack verification failed.");
            var path=Path.Combine(Engine.AppDir,"JUGCRAFT.mrpack"); File.WriteAllBytes(path,bytes);
            Engine.WriteJson(Path.Combine(Engine.AppDir,"import.json"),m);
            Engine.WriteJson(Path.Combine(Engine.AppDir,"binding.json"),new Binding { importPending=true });
            try { OpenPack(path); } catch { Engine.WriteJson(Path.Combine(Engine.AppDir,"binding.json"),new Binding()); throw; }
            Engine.Log("Confirm ONE import as JUGCRAFT in Modrinth. Then choose Select instance folder here.");
        });
    }
    public static void OpenPack(string path) {
        foreach (var hive in new[]{Registry.CurrentUser,Registry.LocalMachine}) foreach(var prefix in new[]{@"Software\Microsoft\Windows\CurrentVersion\Uninstall",@"Software\WOW6432Node\Microsoft\Windows\CurrentVersion\Uninstall"})
        using(var parent=hive.OpenSubKey(prefix)) { if(parent==null) continue; foreach(var name in parent.GetSubKeyNames()) using(var key=parent.OpenSubKey(name)) {
            if((string)key.GetValue("DisplayName")!="Modrinth App") continue;
            string location=key.GetValue("InstallLocation") as string; if(String.IsNullOrEmpty(location)) continue;
            string exe=Path.Combine(location.Trim('"'),"Modrinth App.exe"); if(File.Exists(exe)) { Process.Start(new ProcessStartInfo(exe,"\""+path+"\"") { UseShellExecute=true }); return; }
        }}
        Process.Start(new ProcessStartInfo(path) { UseShellExecute=true });
    }
    void Attach() {
        using(var picker=new FolderBrowserDialog { Description="Select the existing JUGCRAFT instance folder (the folder containing mods and options.txt).", ShowNewFolderButton=false }) {
            if(picker.ShowDialog(this)!=DialogResult.OK) return; var path=picker.SelectedPath;
            Background(()=> { Engine.EnsureStopped(path); var imported=Path.Combine(Engine.AppDir,"import.json"); var manifest=File.Exists(imported)?Engine.Read<Manifest>(imported):Engine.Latest(); Engine.Bind(path,manifest); Engine.Log("Bound to: "+path+"\r\nNow copy the launch hook into this instance's settings."); });
        }
    }
    void Hook() {
        var b=Binding(); Engine.Require(!String.IsNullOrEmpty(b.profile),"Select the JUGCRAFT instance folder first.");
        Engine.Require(!Application.ExecutablePath.Contains("$") && !b.profile.Contains("$"),"Modrinth expands dollar variables in hooks. Use a profile and Windows account path without a dollar sign for this installer.");
        string hook="\""+Application.ExecutablePath.Replace('\\','/')+"\" --check --profile \""+b.profile.Replace('\\','/')+"\"";
        Clipboard.SetText(hook);
        MessageBox.Show(this,"Copied. In JUGCRAFT's instance settings, open Launch hooks and paste into Pre-launch, then save.\r\n\r\nThis is a ONE-TIME setup. Keep existing unrelated hooks; ask for help combining them.\r\n\r\n"+hook,"JUGCRAFT");
    }
    void Check() { var b=Binding(); Engine.Require(!String.IsNullOrEmpty(b.profile),"Select the instance folder first."); Background(()=>Program.Update(b.profile,false)); }
}

public static class Program {
    public static int Update(string profile, bool hook) {
        Engine.NoLinks(profile); Engine.EnsureStopped(profile);
        string work=Engine.Work(profile); Directory.CreateDirectory(work);
        using(var gate=new FileStream(Path.Combine(work,"update.lock"),FileMode.OpenOrCreate,FileAccess.ReadWrite,FileShare.None)) {
            Engine.Recover(profile); var old=Engine.Installed(profile); var next=Engine.Latest();
            var changes=Engine.Plan(profile,old,next);
            if(old.releaseVersion==next.releaseVersion && changes.Count==0) { Engine.Log("Already current: "+next.releaseVersion); return 0; }
            var choice=MessageBox.Show("JUGCRAFT "+next.releaseVersion+" is ready.\r\n\r\nUpdate "+changes.Count+" mod files in your existing instance? Your settings, saves and screenshots will be kept.\r\n\r\nCancel stops this launch.","JUGCRAFT update",MessageBoxButtons.OKCancel,MessageBoxIcon.Information,MessageBoxDefaultButton.Button1,MessageBoxOptions.DefaultDesktopOnly);
            if(choice!=DialogResult.OK) return 2;
            if(hook) {
                Exception failure=null;
                using(var progress=new Form {Text="Updating JUGCRAFT",Size=new Size(540,155),StartPosition=FormStartPosition.CenterScreen,ControlBox=false,FormBorderStyle=FormBorderStyle.FixedDialog}) {
                    var label=new Label {Text="Preparing verified downloads...",Dock=DockStyle.Fill,Padding=new Padding(15),Font=new Font("Segoe UI",11)};
                    progress.Controls.Add(label); progress.Controls.Add(new ProgressBar {Dock=DockStyle.Bottom,Style=ProgressBarStyle.Marquee,Height=20});
                    Engine.Log=s=>progress.Invoke((Action)(()=>label.Text=s));
                    progress.Shown+=(s,e)=>Task.Run(()=> { try {Engine.Apply(profile,next,f=>Engine.Download(f.url,f.size),delegate {});} catch(Exception ex) {failure=ex;} finally {progress.Invoke((Action)(()=>progress.Close()));} });
                    Application.Run(progress);
                }
                if(failure!=null) throw failure;
            } else Engine.Apply(profile,next,f=>Engine.Download(f.url,f.size),delegate {});
            return 0;
        }
    }
    [STAThread] public static int Main(string[] args) {
        ServicePointManager.SecurityProtocol=SecurityProtocolType.Tls12;
        Application.EnableVisualStyles();
        try {
            Engine.NoLinks(Engine.AppDir); Directory.CreateDirectory(Engine.AppDir);
            bool hook=args.Length==3 && args[0]=="--check" && args[1]=="--profile";
            Engine.Require(args.Length==0 || hook,"Unknown arguments.");
            if(hook) {
                var binding=Engine.Read<Binding>(Path.Combine(Engine.AppDir,"binding.json"));
                Engine.Require(String.Equals(Engine.NormalPath(args[2]),Engine.NormalPath(binding.profile),StringComparison.OrdinalIgnoreCase),"This hook is not bound to that JUGCRAFT instance.");
                var current=Environment.GetEnvironmentVariable("INST_DIR");
                if(!String.IsNullOrEmpty(current)) Engine.Require(String.Equals(Engine.NormalPath(current),Engine.NormalPath(binding.profile),StringComparison.OrdinalIgnoreCase),"Use the hook only on its bound JUGCRAFT instance.");
                return Update(binding.profile,true);
            }
            // Stable location keeps the hook working when Downloads is cleaned up.
            string installed=Path.Combine(Engine.AppDir,"JUGCRAFT.exe");
            if(!String.Equals(Application.ExecutablePath,installed,StringComparison.OrdinalIgnoreCase)) {
                Engine.NoLinks(installed); File.Copy(Application.ExecutablePath,installed,true);
                Process.Start(new ProcessStartInfo(installed) { UseShellExecute=true }); return 0;
            }
            bool created;
            using(var gate=new Mutex(true,"Local\\JUGCRAFT-Setup",out created)) {
                Engine.Require(created,"JUGCRAFT setup is already open. Use its existing window.");
                Application.Run(new SetupForm()); return 0;
            }
        } catch(Exception e) {
            MessageBox.Show("JUGCRAFT could not finish safely. Minecraft launch is cancelled.\r\n\r\n"+e.Message+"\r\n\r\nYour settings and saves have not been replaced. Retry when the connection is available, or contact the server owner.","JUGCRAFT",MessageBoxButtons.OK,MessageBoxIcon.Error);
            return 1;
        }
    }
}
}
