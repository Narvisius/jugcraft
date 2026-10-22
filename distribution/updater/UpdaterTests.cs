using System;
using System.IO;
using System.Linq;
using System.Text;
using System.Drawing;
using Jugcraft;

public static class UpdaterTests {
    static int count;
    static void Check(bool b,string message) { if(!b) throw new Exception(message); count++; }
    static void Reject(Action a) { bool failed=false; try { a(); } catch { failed=true; } Check(failed,"Expected rejection"); }
    static byte[] Bytes(string text) { return Encoding.UTF8.GetBytes(text); }
    static FileSpec FileEntry(string name,string value,string version) { var b=Bytes(value); return new FileSpec { path="mods/"+name,sha512=Engine.Hash(b),size=b.Length,policy="managed",url=Engine.ReleasePrefix+"pack-"+version+"/"+name }; }
    static Manifest Make(string version,params FileSpec[] entries) { return new Manifest {schemaVersion=1,updaterVersion=1,releaseVersion=version,sourceCommit=new string('a',40),minecraft="26.3",fabricLoader="0.19.5",java=25,files=entries,pack=new PackSpec {url=Engine.ReleasePrefix+"pack-"+version+"/test.mrpack",size=1,sha512=Engine.Hash(Bytes("x"))}}; }
    static void Put(string profile,FileSpec f,string value) { var p=Engine.Child(profile,f.path); Directory.CreateDirectory(Path.GetDirectoryName(p)); File.WriteAllText(p,value); }
    [STAThread] public static int Main(string[] args) {
        string root=Path.Combine(Path.GetTempPath(),"jugcraft-updater-tests-"+Guid.NewGuid().ToString("N")); Directory.CreateDirectory(root);
        Engine.AppDir=Path.Combine(root,"app"); Directory.CreateDirectory(Engine.AppDir);
        if(args.Length>0 && args[0]=="--render") {
            using(var form=new SetupForm()) using(var bitmap=new Bitmap(form.Width,form.Height)) { form.StartPosition=System.Windows.Forms.FormStartPosition.Manual; form.Location=new Point(-32000,-32000); form.ShowInTaskbar=false; form.Show(); System.Windows.Forms.Application.DoEvents(); form.DrawToBitmap(bitmap,new Rectangle(0,0,form.Width,form.Height)); bitmap.Save(args[1]); form.Close(); } return 0;
        }
        if(args.Length>0 && args[0]=="--remote") { System.Net.ServicePointManager.SecurityProtocol=System.Net.SecurityProtocolType.Tls12; var latest=Engine.Latest(); Console.WriteLine(latest.releaseVersion+": "+latest.files.Length+" verified manifest entries"); return 0; }
        try {
            string profile=Path.Combine(root,"Profile with spaces & Unicode é"); Directory.CreateDirectory(profile);
            var old=Make("test.1",FileEntry("jugcraft.jar","old","test.1"),FileEntry("retired.jar","retired","test.1"));
            Put(profile,old.files[0],"old"); Put(profile,old.files[1],"retired");
            File.WriteAllText(Path.Combine(profile,"options.txt"),"player settings");
            Directory.CreateDirectory(Path.Combine(profile,"saves")); File.WriteAllText(Path.Combine(profile,"saves","world.dat"),"world");
            Directory.CreateDirectory(Path.Combine(profile,"config")); File.WriteAllText(Path.Combine(profile,"config","client.json"),"personal config");
            File.WriteAllText(Path.Combine(profile,"mods","personal.jar"),"personal mod");
            Engine.Bind(profile,old); Check(Engine.Installed(profile).releaseVersion=="test.1","Initial binding");
            var next=Make("test.2",FileEntry("jugcraft.jar","new","test.2"),FileEntry("added.jar","added","test.2"));
            Func<FileSpec,byte[]> fetch=f=>Bytes(f.path=="mods/jugcraft.jar"?"new":"added");
            Reject(()=>Engine.Apply(profile,next,f=>Bytes("corrupt"),delegate {}));
            Check(File.ReadAllText(Path.Combine(profile,"mods","jugcraft.jar"))=="old","Failed hash must not change active mod");
            Reject(()=>Engine.Apply(profile,next,fetch,i=> { if(i==2) throw new IOException("Injected commit failure"); }));
            Check(Engine.Installed(profile).releaseVersion=="test.1","Rollback manifest");
            Check(File.ReadAllText(Path.Combine(profile,"mods","jugcraft.jar"))=="old" && !File.Exists(Path.Combine(profile,"mods","added.jar")),"Rollback changed and newly added files");
            Check(File.Exists(Path.Combine(profile,"mods","retired.jar")),"Rollback retired file");
            Engine.Apply(profile,next,fetch,delegate {});
            Check(Engine.Installed(profile).releaseVersion=="test.2","Commit manifest");
            Check(!File.Exists(Path.Combine(profile,"mods","retired.jar")),"Remove only obsolete owned file");
            Check(File.ReadAllText(Path.Combine(profile,"options.txt"))=="player settings","Options preserved");
            Check(File.ReadAllText(Path.Combine(profile,"config","client.json"))=="personal config","Config preserved");
            Check(File.ReadAllText(Path.Combine(profile,"saves","world.dat"))=="world","Save preserved");
            Check(File.ReadAllText(Path.Combine(profile,"mods","personal.jar"))=="personal mod","Unmanaged mod preserved");
            int downloads=0; Engine.Apply(profile,next,f=> { downloads++; return fetch(f); },delegate {}); Check(downloads==0,"Repeat update downloads nothing");
            File.WriteAllText(Path.Combine(profile,"mods","jugcraft.jar"),"user edit"); Reject(()=>Engine.Plan(profile,next,old)); Check(File.ReadAllText(Path.Combine(profile,"mods","jugcraft.jar"))=="user edit","Conflict preserved");
            File.WriteAllText(Path.Combine(profile,"mods","jugcraft.jar"),"new");
            var extra=Make("test.3",FileEntry("jugcraft.jar","new","test.3"),FileEntry("personal.jar","replace","test.3")); Reject(()=>Engine.Plan(profile,next,extra));
            File.Delete(Path.Combine(profile,"mods","added.jar")); Check(Engine.Plan(profile,next,next).Count==1,"Missing managed file is repairable");
            Engine.Apply(profile,next,fetch,delegate {});
            // Simulate process death after a partial write; recovery uses durable journal.
            var work=Engine.Work(profile); var backup=Engine.Child(Path.Combine(work,"backup"),"mods/jugcraft.jar");
            File.Copy(Path.Combine(profile,"mods","jugcraft.jar"),backup,true);
            Engine.WriteJson(Path.Combine(work,"transaction.json"),new Journal { previous=next,changes=new[]{new Change {path="mods/jugcraft.jar",existed=true}} });
            File.WriteAllText(Path.Combine(profile,"mods","jugcraft.jar"),"partial"); Engine.Recover(profile);
            Check(File.ReadAllText(Path.Combine(profile,"mods","jugcraft.jar"))=="new","Crash recovery");
            foreach(var path in new[]{"../outside.jar","mods/../../outside.jar","mods/C:evil.jar","mods/CON.jar","options.txt","saves/world.jar","mods/sub/evil.jar"}) Reject(()=>Engine.Child(profile,path));
            foreach(var url in new[]{"http://github.com/jimbozoomer-byte/jugcraft/releases/download/x","https://github.com/attacker/repo/x","https://cdn.modrinth.com.evil.test/data/x","https://cdn.modrinth.com/data/a?secret=1","https://raw.githubusercontent.com/SkyblockOverhaul/maven/main/a.jar"}) Reject(()=>Engine.Url(url,true));
            var wrong=Make("test.2",next.files); wrong.minecraft="other"; Reject(()=>Engine.Validate(wrong));
            var duplicate=Make("test.2",next.files[0],next.files[0]); Reject(()=>Engine.Validate(duplicate));
            Check(Engine.NormalPath(@"\\?\C:\Users\Test")==Engine.NormalPath(@"C:\Users\Test"),"Canonical launcher path normalization");
            Console.WriteLine("PASS: "+count+" assertions; fixture directory: "+root); return 0;
        } catch(Exception e) { Console.Error.WriteLine(e); return 1; }
    }
}
