// Jugcraft drone redesign: detailed models, one builder per tier. Forward is +z, the feet stand on y=0.
// Built only from boxes, n-sided prisms and flat extruded outlines so the in-game DroneModel can draw the same parts.
window.JDRONE=function(T,NIGHT){
"use strict";
const V=(x,y,z)=>new T.Vector3(x,y,z), TAU=Math.PI*2, D2R=Math.PI/180;
function ctex(w,h,draw){ const c=document.createElement('canvas'); c.width=w; c.height=h; const x=c.getContext('2d'); draw(x,w,h); const t=new T.CanvasTexture(c); t.encoding=T.sRGBEncoding; t.anisotropy=8; t.wrapS=t.wrapT=T.RepeatWrapping; return t; }
const std=o=>new T.MeshStandardMaterial(o);
const twill=ctex(64,64,(x,w,h)=>{ x.fillStyle='#17191b'; x.fillRect(0,0,w,h); for(let j=0;j<h;j+=8) for(let i=0;i<w;i+=8){ const on=((i+j)/8)%2; x.fillStyle=on?'#262a2e':'#1d2023'; x.fillRect(i,j,8,8); x.fillStyle='rgba(255,255,255,.05)'; on?x.fillRect(i,j,8,2):x.fillRect(i,j,2,8); } });
twill.magFilter=T.NearestFilter;
const shellTex=ctex(64,64,(x,w,h)=>{ x.fillStyle='#2f3439'; x.fillRect(0,0,w,h); const im=x.getImageData(0,0,w,h),d=im.data; for(let i=0;i<d.length;i+=4){ const v=(Math.random()-.5)*7; d[i]+=v; d[i+1]+=v; d[i+2]+=v; } x.putImageData(im,0,0); });
shellTex.magFilter=T.LinearFilter;
const glowCol=(day,night)=>new T.MeshBasicMaterial({color:NIGHT?night:day, toneMapped:false});
const M={
  shell: std({map:shellTex, color:0xffffff, roughness:.55, metalness:.15}),
  shell2: std({color:0x1d2024, roughness:.6, metalness:.15}),
  batt: std({color:0x353a40, roughness:.55, metalness:.2}),
  carbon: std({map:twill, roughness:.35, metalness:.2}),
  anod: std({color:0x30353b, roughness:.4, metalness:.6}),
  steel: std({color:0x7d848b, roughness:.35, metalness:.8}),
  black: std({color:0x0f1112, roughness:.7, metalness:.2}),
  rubber: std({color:0x121314, roughness:.95}),
  prop: std({color:0x1e2124, roughness:.6, metalness:.1}),
  lens: std({color:0x0a1016, roughness:.05, metalness:.9}),
  grey: std({color:0x8e9296, roughness:.55, metalness:.1}),
  tote: std({color:0x34393e, roughness:.6, metalness:.2}),
  amberPaint: std({color:0xc98a3a, roughness:.5, metalness:.2}),
  blur: new T.MeshBasicMaterial({color:0x1a1c1f, transparent:true, opacity:.22, depthWrite:false, side:T.DoubleSide}),
  blurRim: new T.MeshBasicMaterial({color:0x6d7277, transparent:true, opacity:.25, depthWrite:false, side:T.DoubleSide}),
  white: glowCol(0xe9e6de,0xfff6e8), amber: glowCol(0xd59a4a,0xffaa3a), amberDim: glowCol(0x8a6a3c,0xb87a2a),
  tether: std({color:0xd8d4c8, roughness:.8}),
};
// colours above are written as they should look (sRGB); three r128 shades in linear, so convert the unmapped ones
for(const k of ['shell2','batt','anod','steel','black','rubber','prop','lens','grey','tote','amberPaint','tether']) M[k].color.convertSRGBToLinear();
M.blur.color.convertSRGBToLinear(); M.blurRim.color.convertSRGBToLinear();
const glowTex=ctex(64,64,(x,w,h)=>{ const g=x.createRadialGradient(32,32,0,32,32,32); g.addColorStop(0,'rgba(255,255,255,1)'); g.addColorStop(.25,'rgba(255,255,255,.5)'); g.addColorStop(1,'rgba(255,255,255,0)'); x.fillStyle=g; x.fillRect(0,0,w,h); });
function stencil(txt,col='#d59a4a',w=128,h=48,font='700 34px "DejaVu Sans Mono", monospace'){ return new T.MeshBasicMaterial({map:ctex(w,h,(x,W,H)=>{ x.clearRect(0,0,W,H); x.font=font; x.textAlign='center'; x.textBaseline='middle'; x.fillStyle=col; x.fillText(txt,W/2,H/2+2); }), transparent:true, toneMapped:false, depthWrite:false, polygonOffset:true, polygonOffsetFactor:-2}); }

// ---------- part helpers ----------
function kit(g){
  const add=(geo,mat,x,y,z,rx=0,ry=0,rz=0,par=g)=>{ const m=new T.Mesh(geo,mat); m.position.set(x,y,z); m.rotation.set(rx,ry,rz); m.castShadow=true; m.receiveShadow=true; par.add(m); return m; };
  const box=(w,h,d,mat,x,y,z,rx,ry,rz,par)=>add(new T.BoxGeometry(w,h,d),mat,x,y,z,rx,ry,rz,par);
  const cyl=(r,h,seg,mat,x,y,z,rx,ry,rz,par,r2)=>add(new T.CylinderGeometry(r,r2??r,h,seg),mat,x,y,z,rx,ry,rz,par);
  const tube=(a,b,r,mat,seg=8,par=g)=>{ const L=a.distanceTo(b), m=new T.Mesh(new T.CylinderGeometry(r,r,L,seg),mat); m.position.copy(a).add(b).multiplyScalar(.5); m.quaternion.setFromUnitVectors(V(0,1,0),b.clone().sub(a).normalize()); m.castShadow=true; m.receiveShadow=true; par.add(m); return m; };
  // side profile (z,y) extruded across x, centred
  const prof=(pts,width,mat,x=0,par=g)=>{ const s=new T.Shape(pts.map(p=>new T.Vector2(p[0],p[1]))); const ge=new T.ExtrudeGeometry(s,{depth:width,bevelEnabled:false}); ge.rotateY(-Math.PI/2); ge.translate(width/2+x,0,0); return add(ge,mat,0,0,0,0,0,0,par); };
  // top outline (x,z) extruded up
  const plan=(pts,h,mat,y,par=g)=>{ const s=new T.Shape(pts.map(p=>new T.Vector2(p[0],-p[1]))); const ge=new T.ExtrudeGeometry(s,{depth:h,bevelEnabled:false}); ge.rotateX(-Math.PI/2); return add(ge,mat,0,y,0,0,0,0,par); };
  const light=(mat,x,y,z,r=.02,par=g,sz=.28,h=r*.9)=>{ const m=cyl(r,h,r>.03?16:8,mat,x,y,z,0,0,0,par); m.castShadow=false; if(NIGHT){ const sp=new T.Sprite(new T.SpriteMaterial({map:glowTex,color:mat.color,transparent:true,depthWrite:false,blending:T.AdditiveBlending,toneMapped:false})); sp.scale.set(sz,sz,sz); sp.position.set(x,y,z); par.add(sp); } return m; };
  return {add,box,cyl,tube,prof,plan,light};
}

// two-blade prop: tapered blade outline, a few degrees of pitch; spinning shows a blur disc and faint blades
function prop2(par,k,r,spin,dir,phase,n=2,chord=1){
  const g=new T.Group(); par.add(g); g.userData.spin=dir||1;
  const bl=[[.02,-.016],[.07,-.026],[.14,-.024],[r*.8,-.016],[r,-.006],[r,.006],[r*.8,.012],[.14,.016],[.07,.02],[.02,.012]].map(q=>[q[0],q[1]*chord]);
  const ge=new T.ExtrudeGeometry(new T.Shape(bl.map(p=>new T.Vector2(p[0],p[1]))),{depth:.007,bevelEnabled:false}); ge.rotateX(-Math.PI/2);
  const mat=spin?Object.assign(M.prop.clone(),{transparent:true,opacity:.35}):M.prop;
  for(let s=0;s<n;s++){ const p=new T.Group(); p.rotation.y=s*TAU/n+phase; g.add(p); const b=new T.Mesh(ge,mat); b.rotation.x=dir*12*D2R; b.castShadow=!spin; p.add(b);
    const tip=new T.Mesh(new T.BoxGeometry(.03,.008,.03),M.amberPaint); tip.position.set(r-.018,.004,0); tip.visible=!spin; p.add(tip); }
  if(spin){ const d=new T.Mesh(new T.CircleGeometry(r,40),M.blur); d.rotation.x=-Math.PI/2; d.position.y=.004; g.add(d); const rim=new T.Mesh(new T.RingGeometry(r-.03,r,40),M.blurRim); rim.rotation.x=-Math.PI/2; rim.position.y=.005; g.add(rim); }
  return g; }

// ===================================================================== T1 COURIER QUAD
// A present-day parcel quadcopter: carbon tube arms on folding hinges, four outrunner motors with two-blade props,
// a swappable battery sled, GNSS mast, stereo obstacle cameras, a belly winch and a clip-in parcel tote on skids.
function t1(o){
  const g=new T.Group(), K=kit(g), {box,cyl,tube,prof,plan,light,add}=K;
  // hull: belly pan, main shell with a sloped nose and tucked tail, canopy over the electronics bay
  prof([[-.27,.29],[.25,.29],[.32,.35],[.30,.46],[-.21,.46],[-.28,.40]],.40,M.shell);
  prof([[-.26,.285],[.24,.285],[.26,.30],[-.27,.30]],.36,M.shell2);
  prof([[-.01,.46],[.21,.46],[.17,.515],[.01,.515]],.30,M.shell);
  box(.302,.006,.205,M.black,0,.5155,.10); // canopy seam
  for(const s of [-1,1]){ box(.006,.012,.44,M.black,s*.2005,.43,.0); box(.006,.05,.006,M.black,s*.2005,.37,.12); box(.006,.05,.006,M.black,s*.2005,.37,-.14);
    for(let k=0;k<5;k++) box(.012,.05,.012,M.black,s*.199,.355,-.20+k*.022); // cooling vent slats
    const st=new T.Mesh(new T.PlaneGeometry(.11,.04),stencil('CQ-T1')); st.position.set(s*.2015,.43-.03,.05); st.rotation.y=s*Math.PI/2; g.add(st); }
  // nose: stereo obstacle cameras either side of an amber status bar
  for(const s of [-1,1]){ const c=cyl(.022,.02,10,M.anod,s*.09,.40,.317,Math.PI/2-.35); cyl(.015,.022,10,M.lens,s*.09,.401,.319,Math.PI/2-.35); }
  const sb=box(.10,.012,.012,o.on?M.amber:M.amberDim,0,.425,.312,-.35);
  // battery sled in the rear bay: pull handle, latch and four charge pips
  box(.27,.012,.24,M.black,0,.463,-.105); box(.25,.05,.22,M.batt,0,.494,-.105);
  for(let k=0;k<4;k++) box(.022,.008,.014,k<(o.charge??4)?M.white:M.black,-.045+k*.03,.521,-.05);
  tube(V(-.07,.505,-.217),V(-.07,.535,-.217),.007,M.black); tube(V(.07,.505,-.217),V(.07,.535,-.217),.007,M.black); tube(V(-.07,.535,-.217),V(.07,.535,-.217),.007,M.black);
  box(.05,.014,.02,M.amberPaint,0,.505,.01);
  const bl=new T.Mesh(new T.PlaneGeometry(.06,.025),stencil('Li-ion','#bfc3c7')); bl.rotation.x=-Math.PI/2; bl.position.set(.07,.5195,-.15); g.add(bl);
  // GNSS mast and puck on the canopy, two LTE stubs and the white anti-collision strobe at the tail
  cyl(.012,.13,8,M.anod,0,.58,.10); cyl(.016,.02,8,M.anod,0,.525,.10); cyl(.055,.024,14,M.shell2,0,.655,.10); cyl(.05,.006,14,M.grey,0,.67,.10);
  for(const s of [-1,1]) tube(V(s*.16,.46,-.24),V(s*.18,.56,-.29),.008,M.rubber);
  light(M.white,0,.47,-.255,.016);
  // four arms: hinge block with an amber locking collar, carbon tube, motor clamp, ESC pod, motor, prop, nav light
  const props=[];
  for(let k=0;k<4;k++){ const a=(45+90*k)*D2R, sx=Math.sin(a), sz=Math.cos(a), front=sz>0;
    const at=(r,y)=>V(sx*r,y,sz*r);
    const hg=box(.07,.06,.09,M.anod,...at(.255,.40).toArray(),0,a,0); box(.075,.012,.012,M.steel,...at(.255,.42).toArray(),0,a+Math.PI/2,0);
    const col=tube(at(.30,.403),at(.33,.405),.031,M.amberPaint,10);
    tube(at(.27,.40),at(.60,.435),.024,M.carbon,10);
    box(.07,.035,.09,M.anod,...at(.61,.437).toArray(),0,a,0);
    // ESC pod hung under the arm, with fins
    box(.045,.03,.10,M.shell2,...at(.48,.39).toArray(),0,a,0); for(let f=0;f<3;f++) box(.003,.012,.09,M.anod,...at(.48,.37).add(V(Math.cos(a)*(f-1)*.012,0,-Math.sin(a)*(f-1)*.012)).toArray(),0,a,0);
    // outrunner motor: stator base, finned bell, cap, hub
    const mx=sx*.62, mz=sz*.62;
    cyl(.045,.016,14,M.anod,mx,.462,mz); cyl(.056,.05,16,M.black,mx,.495,mz); for(let f=0;f<2;f++) cyl(.058,.005,16,M.anod,mx,.483+f*.02,mz);
    cyl(.05,.008,16,M.anod,mx,.524,mz); cyl(.018,.026,10,M.steel,mx,.538,mz); cyl(.008,.012,6,M.black,mx,.556,mz);
    const p=new T.Group(); p.position.set(mx,.546,mz); g.add(p); prop2(p,k,.36,o.spin,k%2?1:-1,(o.phase??0)+k*0.9+(front?0:.5)); props.push(p);
    // nav light under the motor: white at the front, amber at the back
    light(front?M.white:M.amber,mx,.43,mz,.018);
    cyl(.024,.012,10,M.anod,mx,.448,mz); }
  // skid gear: four splayed struts, two skid tubes with rubber end caps and feet
  for(const s of [-1,1]){ for(const z of [-1,1]){ tube(V(s*.13,.29,z*.13),V(s*.23,.035,z*.17),.016,M.carbon,8); box(.05,.025,.04,M.anod,s*.13,.285,z*.13); box(.04,.04,.04,M.anod,s*.23,.035,z*.17); }
    tube(V(s*.23,.03,-.29),V(s*.23,.03,.29),.017,M.carbon,8); for(const z of [-1,1]) cyl(.021,.035,8,M.rubber,s*.23,.03,z*.29,Math.PI/2);
    for(const z of [-.17,.17]) box(.06,.012,.06,M.rubber,s*.23,.006,z); }
  tube(V(-.23,.07,.17),V(.23,.07,.17),.008,M.anod,6); tube(V(-.23,.07,-.17),V(.23,.07,-.17),.008,M.anod,6);
  // belly: gimbal camera under the nose, landing light, optical-flow sensor, winch with its drum, parcel tote on the latch
  box(.06,.02,.05,M.anod,0,.277,.22); for(const s of [-1,1]) box(.01,.05,.03,M.anod,s*.042,.252,.22);
  add(new T.SphereGeometry(.036,12,8),M.shell2,0,.237,.225); cyl(.018,.012,12,M.lens,0,.232,.258,Math.PI/2);
  light(M.white,0,.282,.16,.012,g,.18);
  box(.045,.008,.045,M.black,0,.282,-.21); cyl(.008,.006,8,M.lens,-.01,.278,-.21); cyl(.008,.006,8,M.lens,.012,.278,-.21);
  box(.15,.03,.15,M.shell2,0,.272,-.02); cyl(.026,.12,12,M.anod,0,.250,-.02,0,0,Math.PI/2); for(const s of [-1,1]) box(.012,.05,.05,M.anod,s*.068,.25,-.02);
  const drop=o.cargo==='winch'?(o.drop??.6):0;
  if(o.cargo){ const tg=new T.Group(); tg.position.set(0,-drop,0); g.add(tg); const TK=kit(tg);
    if(drop>.02){ tube(V(0,.25,-.02),V(0,.235-drop,-.02),.004,M.tether,6); }
    // tote: 0.30 x 0.17 x 0.24, ribbed, amber band, hook bead and latch bar
    TK.box(.30,.17,.24,M.tote,0,.135,-.02); for(const s of [-1,1]) for(let k=0;k<3;k++) TK.box(.006,.15,.012,M.black,s*.151,.135,-.10+k*.08);
    TK.box(.302,.03,.242,M.amberPaint,0,.17,-.02); TK.box(.16,.012,.03,M.anod,0,.226,-.02); TK.cyl(.02,.03,10,M.amberPaint,0,.245,-.02);
    const tl=new T.Mesh(new T.PlaneGeometry(.16,.05),stencil('PARCEL','#e6e2d8',192,48,'700 30px "DejaVu Sans Mono", monospace')); tl.position.set(0,.12,.101); tg.add(tl); }
  g.userData.props=props; return g; }


// ===================================================================== T2 SURVEY HEXACOPTER
// A survey-and-freight hex a step on from T1: a moulded octagonal shell with a lit waist band, faired arms,
// enclosed motor nacelles with LED rings, three-blade props, dual RTK GNSS masts for heading, a lidar puck and a
// five-lens multispectral camera under the nose, side battery packs, a two-parcel pod on the winch, and legs that fold up in flight.
const oct=(a,b,c,cf=c)=>[[a-cf,b],[a,b-cf],[a,-b+c],[a-c,-b],[-a+c,-b],[-a,-b+c],[-a,b-cf],[-a+cf,b]];
function t2(o){
  const g=new T.Group(), K=kit(g), {box,cyl,tube,plan,light,add}=K, on=o.on!==false;
  plan(oct(.22,.30,.08,.10),.03,M.shell2,.30);
  plan(oct(.25,.34,.09,.12),.07,M.shell,.33);
  const band=plan(oct(.256,.346,.09,.12),.012,on?M.amber:M.amberDim,.352); band.castShadow=false;
  plan(oct(.232,.31,.08,.11),.05,M.shell,.40);
  plan(oct(.19,.23,.07,.08),.035,M.shell,.45);
  plan(oct(.13,.15,.05,.05),.018,M.shell2,.485);
  for(let k=0;k<5;k++) box(.18,.006,.012,M.black,0,.504,-.07+k*.035); // cooling grille on the crown
  // nose visor: dark glass with three lenses (stereo pair and a ranging lens)
  box(.22,.034,.012,M.lens,0,.381,.345); for(const x of [-.07,0,.07]) cyl(.013,.01,10,x?M.anod:M.amberPaint,x,.381,.352,Math.PI/2);
  // side battery packs, latches, four charge pips each, SV-T2 stencils
  for(const sd of [-1,1]){ box(.026,.046,.20,M.batt,sd*.235,.425,-.04); box(.03,.012,.03,M.amberPaint,sd*.236,.43,.07);
    for(let k=0;k<4;k++) box(.01,.01,.02,k<(o.charge??4)?M.white:M.black,sd*.249,.425,-.10+k*.035);
    const st=new T.Mesh(new T.PlaneGeometry(.12,.04),stencil('SV-T2')); st.position.set(sd*.2525,.385-.04,-.16); st.rotation.y=sd*Math.PI/2; g.add(st); }
  // dual RTK GNSS masts (fore and aft give heading), white strobe between them, one LTE aerial
  for(const z of [.09,-.11]){ cyl(.011,.10,8,M.anod,0,.55,z); cyl(.05,.022,14,M.shell2,0,.61,z); cyl(.045,.006,14,M.grey,0,.624,z); }
  light(M.white,0,.512,-.01,.014); tube(V(.10,.47,-.24),V(.12,.57,-.30),.008,M.rubber);
  // six faired arms with fold joints, nacelles, LED rings, three-blade props
  const props=[];
  for(let k=0;k<6;k++){ const a=(30+60*k)*D2R, front=Math.cos(a)>.5; const ag=new T.Group(); ag.rotation.y=a; g.add(ag); const AK=kit(ag);
    AK.box(.10,.06,.06,M.anod,0,.38,.255); AK.box(.102,.008,.062,M.amberPaint,0,.38,.255);
    AK.plan([[-.042,.28],[.042,.28],[.026,.66],[-.026,.66]],.04,M.shell,.362);
    AK.plan([[-.012,.29],[.012,.29],[.008,.65],[-.008,.65]],.008,M.shell2,.402);
    AK.cyl(.056,.075,16,M.shell,0,.418,.70); AK.cyl(.058,.012,16,M.shell2,0,.45,.70);
    AK.light(front?M.white:M.amber,0,.376,.70,.05,ag,.32,.012); AK.cyl(.04,.02,12,M.shell2,0,.372,.70);
    AK.cyl(.048,.03,16,M.black,0,.47,.70); AK.cyl(.02,.02,10,M.steel,0,.492,.70);
    const p=new T.Group(); p.position.set(0,.5,.70); ag.add(p); prop2(p,k,.28,o.spin,k%2?1:-1,(o.phase??0)+k*1.3,3,.9); props.push(p); }
  // telescoping legs: a sleeve under each side and a sliding strut pair to the skid; o.gearUp draws them up under the hull
  for(const sd of [-1,1]){ const lg=new T.Group(); lg.position.set(sd*.19,.31,0); g.add(lg); const LK=kit(lg); const up=o.gearUp?.2:0;
    LK.box(.04,.03,.30,M.anod,0,-.01,0); LK.box(.042,.008,.302,M.amberPaint,0,.008,0);
    for(const z of [-1,1]){ LK.tube(V(0,-.01,z*.12),V(sd*.045,-.12,z*.135),.022,M.shell2,8); LK.tube(V(sd*.04,-.11,z*.133),V(sd*.11,-.29+up,z*.16),.015,M.carbon,8); }
    LK.tube(V(sd*.11,-.29+up,-.25),V(sd*.11,-.29+up,.25),.017,M.carbon,8); for(const z of [-1,1]) LK.cyl(.021,.03,8,M.rubber,sd*.11,-.29+up,z*.25,Math.PI/2);
    for(const z of [-.16,.16]) LK.box(.06,.012,.07,M.rubber,sd*.11,-.304+up,z); }
  // belly: lidar puck under the nose, multispectral camera, radar altimeter, landing light, winch and the two-parcel pod
  box(.07,.02,.07,M.anod,0,.29,.21); cyl(.055,.06,16,M.shell2,0,.25,.21); cyl(.057,.022,16,M.lens,0,.245,.21); cyl(.045,.01,16,M.anod,0,.216,.21);
  box(.13,.03,.07,M.shell2,0,.284,.07); for(const [x,z] of [[-.04,.055],[0,.055],[.04,.055],[-.02,.085],[.02,.085]]) cyl(.011,.008,10,M.lens,x,.268,z);
  box(.06,.01,.04,M.black,.10,.297,-.24); light(M.white,0,.296,.30,.012,g,.16);
  box(.16,.03,.14,M.shell2,0,.285,-.05); cyl(.026,.12,12,M.anod,0,.268,-.05,0,0,Math.PI/2);
  const drop=o.cargo==='winch'?(o.drop??.6):0;
  if(o.cargo){ const tg=new T.Group(); tg.position.set(0,-drop,-.05); g.add(tg); const TK=kit(tg);
    if(drop>.02) tube(V(0,.26,-.05),V(0,.245-drop,-.05),.004,M.tether,6);
    TK.plan(oct(.16,.14,.03),.15,M.tote,.075); TK.plan(oct(.162,.142,.03),.025,M.amberPaint,.19); TK.plan(oct(.163,.143,.03),.006,M.black,.13); 
    for(const sd of [-1,1]) TK.box(.03,.04,.012,M.anod,sd*.07,.16,.145);
    TK.box(.16,.012,.03,M.anod,0,.232,0); TK.cyl(.02,.03,10,M.amberPaint,0,.252,0);
    const tl=new T.Mesh(new T.PlaneGeometry(.16,.045),stencil('CARGO x2','#e6e2d8',192,48,'700 30px "DejaVu Sans Mono", monospace')); tl.position.set(0,.105,.1405); tg.add(tl); }
  g.userData.props=props; return g; }

// ===================================================================== T3 LIFTER OCTOCOPTER
// A coaxial X8 heavy lifter: four blended arms, each ending in a pod with a motor and prop on top and another below
// (eight rotors in a quad's footprint), outer prop guards, a lit spine on each arm, an aluminium-skinned lifting body
// with its GNSS under a flush radome, cartridge batteries in the tail, and a four-jaw grapple holding a sealed crate.
const crateTex=ctex(128,128,(x,w,h)=>{ x.fillStyle='#4a5157'; x.fillRect(0,0,w,h); x.fillStyle='#363c41'; for(let i=0;i<w;i+=16) x.fillRect(i,0,3,h); x.fillStyle='#d9962f'; x.fillRect(0,50,w,24); x.fillStyle='#1b1d1f'; x.font='700 15px "DejaVu Sans Mono", monospace'; x.textAlign='center'; x.fillText('SEALED',w/2,67); x.strokeStyle='#262a2e'; x.lineWidth=8; x.strokeRect(4,4,w-8,h-8); });
M.crate=std({map:crateTex, roughness:.6, metalness:.25});
M.alu=std({color:0x5d646b, roughness:.38, metalness:.75}); M.alu.color.convertSRGBToLinear();
const scl=(pts,f)=>pts.map(q=>[q[0]*f,q[1]*f]);
function t3(o){
  const g=new T.Group(), K=kit(g), {box,cyl,tube,plan,light,add}=K, on=o.on!==false;
  const HULL=[[.16,.42],[.27,.30],[.30,.02],[.28,-.28],[.20,-.36],[-.20,-.36],[-.28,-.28],[-.30,.02],[-.27,.30],[-.16,.42]];
  plan(scl(HULL,.9),.04,M.shell2,.50); plan(HULL,.10,M.alu,.54);
  const band=plan(scl(HULL,1.012),.012,on?M.amber:M.amberDim,.575); band.castShadow=false;
  plan(scl(HULL,.88),.06,M.shell,.64); plan(scl(HULL,.66),.025,M.alu,.70);
  for(const z of [.18,-.06]) box(.36,.004,.006,M.black,0,.7255,z); // panel seams on the crown
  cyl(.10,.02,20,M.shell2,0,.735,.02); cyl(.085,.018,20,M.grey,0,.75,.02); cyl(.05,.008,16,M.grey,0,.762,.02); // flush GNSS radome
  light(M.white,0,.73,-.20,.014);
  // nose visor across the front: dark glass, four sensors
  box(.30,.034,.012,M.lens,0,.615,.425); for(const x of [-.10,-.035,.035,.10]) cyl(.012,.01,10,Math.abs(x)<.05?M.anod:M.amberPaint,x,.615,.432,Math.PI/2);
  // side intakes and stencils
  for(const sd of [-1,1]){ for(let k=0;k<6;k++) box(.006,.05,.012,M.black,sd*.301,.605,-.12+k*.03);
    const st=new T.Mesh(new T.PlaneGeometry(.12,.04),stencil('LF-T3')); st.position.set(sd*.3025,.605-.0,.16); st.rotation.y=sd*Math.PI/2; st.rotation.z=0; g.add(st); }
  // tail: two battery cartridges with five-bar gauges, amber tail light bar
  for(const sd of [-1,1]){ box(.13,.06,.03,M.batt,sd*.08,.60,-.37); for(let k=0;k<5;k++) box(.016,.008,.004,k<(o.charge??5)?M.white:M.black,sd*.08-.04+k*.02,.615,-.386); box(.02,.03,.006,M.amberPaint,sd*.08+.05,.59,-.387); }
  box(.30,.012,.01,on?M.amber:M.amberDim,0,.556,-.36);
  // four blended arms with lit spines; coaxial pods: nacelle, motor and prop on top, motor and prop below; outer guard arcs
  const props=[];
  for(let k=0;k<4;k++){ const a=(45+90*k)*D2R, front=Math.cos(a)>0; const ag=new T.Group(); ag.rotation.y=a; g.add(ag); const AK=kit(ag);
    AK.plan([[-.08,.22],[.08,.22],[.045,.74],[-.045,.74]],.07,M.alu,.545);
    AK.plan([[-.06,.24],[.06,.24],[.035,.73],[-.035,.73]],.012,M.shell,.615);
    AK.plan([[-.06,.22],[.06,.22],[.032,.72],[-.032,.72]],.02,M.shell2,.525); AK.box(.17,.06,.05,M.shell2,0,.57,.235);
    const sp=AK.plan([[-.008,.27],[.008,.27],[.006,.70],[-.006,.70]],.006,on?M.amber:M.amberDim,.627); sp.castShadow=false;
    AK.cyl(.068,.22,16,M.shell,0,.59,.80); AK.cyl(.07,.04,16,M.alu,0,.59,.80); AK.light(front?M.white:M.amber,0,.59,.80,.072,ag,.34,.012);
    for(const [y,dir] of [[.72,1],[.46,-1]]){ AK.cyl(.056,.04,16,M.black,0,y,.80); AK.cyl(.02,.022,10,M.steel,0,y+dir*.03,.80);
      const p=new T.Group(); p.position.set(0,y+dir*.045,.80); ag.add(p); prop2(p,k,.30,o.spin,(k+(dir>0?0:1))%2?1:-1,(o.phase??0)+k*1.1+(dir>0?0:.7),2,1.15); props.push(p); }
    const gd=new T.Mesh(new T.TorusGeometry(.335,.012,6,28,Math.PI*.9),M.carbon); gd.rotation.set(Math.PI/2,0,Math.PI*.05); gd.position.set(0,.59,.80); gd.castShadow=true; ag.add(gd);
    for(const s2 of [-1,1]){ const e=V(Math.sin(s2*1.35)*.335,.59,.80+Math.cos(s2*1.35)*.335); AK.tube(V(Math.sin(s2*1.35)*.07,.59,.80+Math.cos(s2*1.35)*.07),e,.009,M.carbon,6); } }
  // four legs splayed from the belly to pads
  for(const [x,z] of [[1,1],[1,-1],[-1,1],[-1,-1]]){ tube(V(x*.24,.52,z*.22),V(x*.32,.03,z*.29),.02,M.shell2,8); tube(V(x*.24,.52,z*.22),V(x*.32,.03,z*.29),.023,M.alu,8).scale.set(1,.25,1);
    box(.08,.02,.08,M.rubber,x*.32,.01,z*.29); box(.05,.03,.05,M.anod,x*.24,.515,z*.22); }
  // grapple: four jaws on rams round a sealed crate (four blocks' worth)
  box(.30,.03,.30,M.anod,0,.49,-.02);
  const drop=o.cargo==='winch'?(o.drop??0):0;
  const cg=new T.Group(); cg.position.set(0,-drop,0); g.add(cg); const CK=kit(cg);
  if(o.cargo){ crateBox(CK,.40,.40,.40,0,.27,-.02); }
  for(const [x,z] of [[1,1],[1,-1],[-1,1],[-1,-1]]){ const jx=x*.215, jz=z*.215-.02;
    tube(V(x*.12,.49,z*.12-.02),V(jx,.47,jz),.012,M.steel,6);
    box(.03,.16,.03,M.shell2,jx,.40,jz); box(.034,.012,.034,M.amberPaint,jx,.46,jz); box(.022,.05,.022,M.anod,jx-x*.012,.34,jz-z*.012); }
  light(M.white,0,.49,.30,.012,g,.16);
  g.userData.props=props; return g; }

// ===================================================================== T4 CONCEPTS (medium, ducted fans; looks free from here on)
// shared ducted-fan internals: hub and spinner, fan blades (or a blur when spinning), stators below, exit louvres
function fanCore(par,R,H,o,k=0){ const K=kit(par);
  K.cyl(R*.22,H*.7,14,M.shell2,0,H*.45,0); K.cyl(R*.08,H*.12,14,M.anod,0,H*.86,0,0,0,0,par,R*.2);
  if(o.spin){ const d=new T.Mesh(new T.CircleGeometry(R*.98,36),M.blur); d.rotation.x=-Math.PI/2; d.position.y=H*.62; par.add(d); }
  const bg=new T.Group(); bg.userData.spin=1; par.add(bg); const n=9; for(let i=0;i<n;i++){ const a=i/n*TAU+(o.phase??0)+k; const b=new T.Mesh(new T.BoxGeometry(R*.72,.008,R*.16),o.spin?Object.assign(M.prop.clone(),{transparent:true,opacity:.3}):M.prop);
    b.position.set(Math.cos(a)*R*.6,H*.62,Math.sin(a)*R*.6); b.rotation.set(0,-a,.45); b.castShadow=!o.spin; bg.add(b); }
  for(let i=0;i<4;i++){ const a=i/4*TAU+.4; K.box(R*.76,H*.14,.012,M.anod,Math.cos(a)*R*.6,H*.28,Math.sin(a)*R*.6,0,-a,0); }
  for(const x of [-.6,-.3,0,.3,.6]){ const L=2*Math.sqrt(1-x*x)*R*.97; K.box(.012,H*.14,L,M.shell2,x*R,H*.07,0,0,0,(o.vane??0)); } }
// a round duct: lathe ring with a rounded intake lip, lit lip line
function ductRing(par,R,H,mat,lip){ const pts=[[R,.0],[R+.02,-.008],[R+.04,.015],[R+.045,H*.7],[R+.035,H],[R+.015,H+.018],[R-.004,H+.008],[R-.006,H*.75],[R,.0]].map(q=>new T.Vector2(q[0],q[1]));
  const m=new T.Mesh(new T.LatheGeometry(pts,40),mat); m.castShadow=true; m.receiveShadow=true; par.add(m);
  if(lip){ const t=new T.Mesh(new T.TorusGeometry(R+.02,.007,6,48),lip); t.rotation.x=Math.PI/2; t.position.y=H+.016; par.add(t); } return m; }
M.duct=std({map:shellTex, color:0xffffff, roughness:.5, metalness:.2, side:T.DoubleSide});

// A  FAN-IN-WING DELTA: a flat arrowhead lifting body with four fans buried in it, louvres that vector the flow
//    for forward speed, twin canted fins, an internal cargo bay with belly doors, tricycle gear (real basis: Ryan XV-5).
function t4a(o){
  const g=new T.Group(), K=kit(g), {box,cyl,tube,plan,light}=K, on=o.on!==false;
  const OUT=[[0,1.65],[.28,1.35],[.55,.8],[1.25,-.45],[1.25,-.70],[.55,-.75],[.42,-1.05],[-.42,-1.05],[-.55,-.75],[-1.25,-.70],[-1.25,-.45],[-.55,.8],[-.28,1.35]];
  const FANS=[[.44,.30,.25],[-.44,.30,.25],[.80,-.36,.28],[-.80,-.36,.28]];
  const sh=new T.Shape(OUT.map(q=>new T.Vector2(q[0],-q[1]))); for(const [x,z,r] of FANS){ const h=new T.Path(); h.absarc(x,-z,r+.02,0,TAU,true); sh.holes.push(h); }
  const wg=new T.ExtrudeGeometry(sh,{depth:.12,bevelEnabled:false,curveSegments:24}); wg.rotateX(-Math.PI/2); K.add(wg,M.shell,0,.42,0);
  const sh2=new T.Shape(scl(OUT,.995).map(q=>new T.Vector2(q[0],-q[1]))); for(const [x,z,r] of FANS){ const h=new T.Path(); h.absarc(x,-z,r+.05,0,TAU,true); sh2.holes.push(h); }
  const wb=new T.ExtrudeGeometry(sh2,{depth:.03,bevelEnabled:false,curveSegments:24}); wb.rotateX(-Math.PI/2); K.add(wb,M.shell2,0,.39,0);
  const sh3=new T.Shape(scl(OUT,.96).map(q=>new T.Vector2(q[0],-q[1]+.02))); for(const [x,z,r] of FANS){ const h=new T.Path(); h.absarc(x,-z,r+.06,0,TAU,true); sh3.holes.push(h); }
  const wt=new T.ExtrudeGeometry(sh3,{depth:.025,bevelEnabled:false,curveSegments:24}); wt.rotateX(-Math.PI/2); K.add(wt,M.shell,0,.54,0);
  for(const [x,z,r] of FANS){ const rs=new T.Shape(); rs.absarc(0,0,r+.06,0,TAU,false); const hh=new T.Path(); hh.absarc(0,0,r+.02,0,TAU,true); rs.holes.push(hh);
    const rg=new T.ExtrudeGeometry(rs,{depth:.018,bevelEnabled:false,curveSegments:32}); rg.rotateX(-Math.PI/2); K.add(rg,M.shell2,x,.54,z); }
  for(const sd of [-1,1]) box(.004,.004,.42,M.black,sd*.30,.5655,-.62);
  // spine: raised centre body, stepped crown, nose sensor glass
  const SP=[[0,1.62],[.14,1.30],[.13,.62],[.13,-.05],[.17,-1.0],[-.17,-1.0],[-.13,-.05],[-.13,.62],[-.14,1.30]];
  plan(SP,.16,M.shell,.54); plan(scl(SP,.75),.06,M.shell2,.70); plan([[0,1.55],[.10,1.28],[.10,.95],[-.10,.95],[-.10,1.28]],.012,M.lens,.70);
  plan([[-.012,.9],[.012,.9],[.012,-.9],[-.012,-.9]],.006,on?M.amber:M.amberDim,.76);
  // fans in the wing: duct lining, lit lip, fan core with louvres angled back
  for(const [x,z,r] of FANS){ const fg=new T.Group(); fg.position.set(x,.36,z); g.add(fg);
    const l=new T.Mesh(new T.CylinderGeometry(r+.02,r+.02,.18,40,1,true),M.duct); l.position.y=.09; fg.add(l);
    const t=new T.Mesh(new T.TorusGeometry(r+.035,.009,6,48),on?(z>0?M.white:M.amber):M.amberDim); t.rotation.x=Math.PI/2; t.position.y=.185; fg.add(t);
    fanCore(fg,r,.18,{...o,vane:o.cruise?.5:0},x); }
  // leading-edge light strips, nose light, wingtip lights
  for(const sd of [-1,1]){ const a=V(sd*.58,.48,.76), b=V(sd*1.22,.48,-.42); const L=a.distanceTo(b); const m=box(L,.014,.012,on?M.amber:M.amberDim,(a.x+b.x)/2,.48,(a.z+b.z)/2+.008); m.rotation.y=Math.atan2(-(b.z-a.z),b.x-a.x); m.castShadow=false;
    light(M.amber,sd*1.26,.48,-.58,.016); box(.012,.08,.22,M.shell2,sd*1.255,.48,-.58); }
  light(M.white,0,.60,1.63,.016);
  // twin canted fins
  for(const sd of [-1,1]){ const f=new T.Shape([[-1.04,0],[-.50,0],[-.80,.34],[-1.02,.36]].map(q=>new T.Vector2(q[0],q[1]))); const fgeo=new T.ExtrudeGeometry(f,{depth:.03,bevelEnabled:false}); fgeo.rotateY(-Math.PI/2); fgeo.translate(.015,0,0);
    const fm=new T.Mesh(fgeo,M.shell); fm.position.set(sd*.30,.54,0); fm.rotation.z=-sd*.35; fm.castShadow=true; g.add(fm);
    const tip=new T.Mesh(new T.BoxGeometry(.034,.02,.15),M.amberPaint); tip.position.set(0,.35,-.88); fm.add(tip); }
  // belly cargo bay: doors with seams; open in flight shows a crate on its cradle lowering
  plan([[.30,.7],[.30,-.6],[-.30,-.6],[-.30,.7]],.05,M.shell2,.34); box(.004,.006,1.2,M.black,0,.338,.05); box(.58,.006,.004,M.black,0,.338,.7);
  if(o.cargo==='winch'){ const d=o.drop??.6; for(const sd of [-1,1]) for(const z of [-.4,.5]) tube(V(sd*.22,.34,z),V(sd*.22,.34-d+.36,z),.008,M.steel,6); K.box(.44,.04,.9,M.anod,0,.34-d+.36,.05); for(let i=0;i<2;i++) crateBox(K,.40,.40,.40,0,.34-d+.14,-.17+i*.44); }
  // tricycle gear (drawn up in flight)
  if(!o.gearUp){ for(const [x,z] of [[0,1.05],[.42,-.55],[-.42,-.55]]){ tube(V(x,.40,z),V(x,.05,z),.025,M.anod,8); box(.10,.05,.14,M.rubber,x,.025,z); box(.06,.02,.06,M.amberPaint,x,.30,z); } }
  return g; }

// B  TILT QUAD-DUCT RUNNER: a slim fuselage with fore and aft stub wings, a tilting duct at each wingtip
//    (up for hover, forward for a fast run), ventral cargo rack, skids (real basis: Bell X-22).
function t4b(o){
  const g=new T.Group(), K=kit(g), {box,cyl,tube,plan,prof,light}=K, on=o.on!==false;
  const W=.52;
  prof([[-1.55,.42],[1.20,.42],[1.55,.56],[1.45,.72],[1.0,.86],[-1.25,.86],[-1.55,.70]],W,M.shell);
  prof([[-1.40,.86],[.95,.86],[.80,.94],[-1.20,.94]],W*.7,M.shell2);
  prof([[.98,.872],[1.462,.732],[1.50,.66],[1.42,.66],[.98,.84]],W*.8,M.lens,0);
  for(const sd of [-1,1]){ const b=box(.004,.014,2.5,on?M.amber:M.amberDim,sd*(W/2+.003),.62,-.1); b.castShadow=false; }
  for(const sd of [-1,1]) for(let k=0;k<6;k++) box(.008,.10,.014,M.black,sd*(W/2+.002),.74,-.7+k*.05);
  // stub wings and tilting ducts
  const tilt=(o.cruise?1.25:0);
  for(const [z,span,front] of [[.85,1.0,true],[-.95,1.1,false]]){ for(const sd of [-1,1]){
      K.plan([[sd*.2,z+.22],[sd*span,z+.15],[sd*span,z-.15],[sd*.2,z-.25]].map(q=>[q[0],q[1]]),.07,M.shell,.62);
      const pv=new T.Group(); pv.position.set(sd*(span+.43),.66,z); pv.rotation.x=tilt; g.add(pv);
      const PK=kit(pv); PK.box(.10,.10,.10,M.anod,-sd*.42,0,0); PK.tube(V(-sd*.42,0,0),V(-sd*.36,0,0),.04,M.steel,10);
      const dg=new T.Group(); dg.position.y=-.16; pv.add(dg); ductRing(dg,.36,.30,M.duct,on?(front?M.white:M.amber):M.amberDim); fanCore(dg,.36,.30,o,sd+z);
      PK.box(.05,.30,.06,M.shell2,-sd*.38,-.01,0); } }
  // ventral cargo rack with two crates, skids
  box(.42,.05,1.0,M.anod,0,.40,-.05); for(const z of [-.27,.22]) crateBox(K,.40,.40,.40,0,.18,z);
  for(const z of [-.27,.22]) for(const sd of [-1,1]) box(.03,.10,.03,M.amberPaint,sd*.215,.35,z);
  if(!o.gearUp) for(const sd of [-1,1]){ tube(V(sd*.38,.02,-1.1),V(sd*.38,.02,1.0),.022,M.carbon,8); for(const z of [-.7,.6]) tube(V(sd*.2,.44,z),V(sd*.38,.03,z),.02,M.carbon,8); }
  light(M.white,0,.58,1.56,.016); light(M.amber,0,.80,-1.55,.016);
  const st=new T.Mesh(new T.PlaneGeometry(.18,.06),stencil('RN-T4')); st.position.set(W/2+.004,.70,.30); st.rotation.y=Math.PI/2; g.add(st);
  return g; }

// C  TANDEM DUCT MULE: two big lift fans in line inside a flat body, louvres for forward thrust, two pusher fans at
//    the tail, saddle cargo pods along both sides (real basis: Urban Aeronautics Cormorant / AirMule).
function t4c(o){
  const g=new T.Group(), K=kit(g), {box,cyl,tube,plan,light}=K, on=o.on!==false;
  const R=.55, FZ=[.78,-.62];
  const OUT=[[.30,1.58],[.66,1.30],[.72,1.0],[.72,-1.10],[.55,-1.45],[-.55,-1.45],[-.72,-1.10],[-.72,1.0],[-.66,1.30],[-.30,1.58]];
  const sh=new T.Shape(OUT.map(q=>new T.Vector2(q[0],-q[1]))); for(const z of FZ){ const h=new T.Path(); h.absarc(0,-z,R+.03,0,TAU,true); sh.holes.push(h); }
  const bg=new T.ExtrudeGeometry(sh,{depth:.34,bevelEnabled:false,curveSegments:32}); bg.rotateX(-Math.PI/2); K.add(bg,M.shell,0,.34,0);
  const sh2=new T.Shape(OUT.map(q=>new T.Vector2(q[0]*.9,-q[1]*.96))); for(const z of FZ){ const h=new T.Path(); h.absarc(0,-z,R+.07,0,TAU,true); sh2.holes.push(h); }
  const tg2=new T.ExtrudeGeometry(sh2,{depth:.04,bevelEnabled:false,curveSegments:32}); tg2.rotateX(-Math.PI/2); K.add(tg2,M.shell,0,.68,0);
  for(const z of FZ){ const rs=new T.Shape(); rs.absarc(0,0,R+.07,0,TAU,false); const hh=new T.Path(); hh.absarc(0,0,R+.03,0,TAU,true); rs.holes.push(hh); const rg=new T.ExtrudeGeometry(rs,{depth:.03,bevelEnabled:false,curveSegments:40}); rg.rotateX(-Math.PI/2); K.add(rg,M.shell2,0,.68,z); }
  // central keel between the fans with the avionics bay, sensor nose and a lit spine
  plan([[.16,.20],[.16,-.04],[-.16,-.04],[-.16,.20]],.10,M.shell2,.72); box(.03,.012,.22,on?M.amber:M.amberDim,0,.825,.08);
  plan([[.27,1.535],[.58,1.30],[-.58,1.30],[-.27,1.535]],.02,M.lens,.70);
  { const b=box(1.06,.014,.006,on?M.amber:M.amberDim,0,.56,-1.453); b.castShadow=false; }
  for(const z of FZ){ const fg=new T.Group(); fg.position.set(0,.32,z); g.add(fg);
    const l=new T.Mesh(new T.CylinderGeometry(R+.03,R+.03,.36,48,1,true),M.duct); l.position.y=.18; fg.add(l);
    const t=new T.Mesh(new T.TorusGeometry(R+.045,.01,6,56),on?(z>0?M.white:M.amber):M.amberDim); t.rotation.x=Math.PI/2; t.position.y=.36; fg.add(t);
    fanCore(fg,R,.34,{...o,vane:o.cruise?.45:0},z); }
  // saddle pods along both sides, two crates each behind roll-up doors
  for(const sd of [-1,1]){ plan([[sd*.72,1.05],[sd*.92,.98],[sd*1.02,.80],[sd*1.02,-.85],[sd*.72,-.95]],.34,M.shell2,.24);
    plan([[sd*.73,1.04],[sd*.925,.975],[sd*1.026,.80],[sd*1.026,-.84],[sd*.73,-.94]],.012,on?M.amber:M.amberDim,.55);
    for(const z of [-.40,.40]){ box(.006,.26,.62,M.batt,sd*1.023,.38,z); for(let k=0;k<6;k++) box(.008,.006,.6,M.black,sd*1.026,.27+k*.045,z); box(.008,.03,.08,M.amberPaint,sd*1.027,.30,z+.25); } }
  // two pusher fans at the tail on a short pylon
  for(const sd of [-1,1]){ const pg=new T.Group(); pg.position.set(sd*.40,.62,-1.55); pg.rotation.x=Math.PI/2; g.add(pg); ductRing(pg,.18,.20,M.duct,on?M.amber:M.amberDim); fanCore(pg,.18,.20,o,sd);
    box(.10,.10,.30,M.shell2,sd*.40,.62,-1.40); }
  if(!o.gearUp) for(const [x,z] of [[.55,1.05],[-.55,1.05],[.55,-1.05],[-.55,-1.05]]){ tube(V(x,.30,z),V(x*1.1,.03,z*1.05),.03,M.anod,8); box(.12,.03,.12,M.rubber,x*1.1,.015,z*1.05); }
  light(M.white,0,.52,1.59,.016);
  const st=new T.Mesh(new T.PlaneGeometry(.18,.06),stencil('ML-T4')); st.rotation.x=-Math.PI/2; st.position.set(0,.7205,-1.25); g.add(st);
  return g; }

// ===================================================================== T4 FAN-WING RUNNER (Justin picked concept A)
// A flat arrowhead lifting body with four fans buried in the wing (lit lips, nine-blade rotors, stators and exit louvres
// that tilt back to drive it forward), a stepped spine with a glass sensor nose, twin canted fins, down-turned
// wingtips, a conformal belly cargo pod whose doors open to lower a cradle of crates, an EO/IR turret, tricycle gear.
function edgeStrip(K,a,b,out,y,mat,h=.016){ const d=V(b[0]-a[0],0,b[1]-a[1]), L=d.length(); d.normalize(); const n=V(d.z,0,-d.x).multiplyScalar(out);
  const m=K.box(L,h,.012,mat,(a[0]+b[0])/2+n.x*.007,y,(a[1]+b[1])/2+n.z*.007); m.rotation.y=Math.atan2(-d.z,d.x); m.castShadow=false; return m; }
function ring(K,x,y,z,r0,r1,h,mat){ const rs=new T.Shape(); rs.absarc(0,0,r1,0,TAU,false); const hh=new T.Path(); hh.absarc(0,0,r0,0,TAU,true); rs.holes.push(hh);
  const rg=new T.ExtrudeGeometry(rs,{depth:h,bevelEnabled:false,curveSegments:40}); rg.rotateX(-Math.PI/2); return K.add(rg,mat,x,y,z); }
function slab(K,outline,holes,y,h,mat){ const sh=new T.Shape(outline.map(q=>new T.Vector2(q[0],-q[1]))); for(const [x,z,r] of holes){ const hp=new T.Path(); hp.absarc(x,-z,r,0,TAU,true); sh.holes.push(hp); }
  const ge=new T.ExtrudeGeometry(sh,{depth:h,bevelEnabled:false,curveSegments:28}); ge.rotateX(-Math.PI/2); return K.add(ge,mat,0,y,0); }
function t4(o){
  const g=new T.Group(), K=kit(g), {box,cyl,tube,plan,light,add}=K, on=o.on!==false, A=on?M.amber:M.amberDim;
  const OUT=[[0,1.65],[.28,1.35],[.55,.8],[1.25,-.45],[1.25,-.70],[.55,-.75],[.42,-1.05],[-.42,-1.05],[-.55,-.75],[-1.25,-.70],[-1.25,-.45],[-.55,.8],[-.28,1.35]];
  const FANS=[[.44,.30,.25],[-.44,.30,.25],[.80,-.36,.28],[-.80,-.36,.28]];
  // wing: dark underside, main skin, bevelled top skin, dark surround round each fan
  slab(K,scl(OUT,.985),FANS.map(f=>[f[0],f[1],f[2]+.05]),.39,.03,M.shell2);
  slab(K,OUT,FANS.map(f=>[f[0],f[1],f[2]+.02]),.42,.10,M.shell);
  slab(K,scl(OUT,.955).map(q=>[q[0],q[1]+.02]),FANS.map(f=>[f[0],f[1],f[2]+.06]),.52,.025,M.shell);
  for(const [x,z,r] of FANS) ring(K,x,.52,z,r+.02,r+.057,.018,M.shell2);
  for(const [x,z,L,ry] of [[.30,-.40,.62,0],[-.30,-.40,.62,0],[.62,.02,.30,.6],[-.62,.02,.30,-.6]]) box(.004,.004,L,M.black,x,.5465,z,0,ry,0);
  // leading-edge light strips on the edge faces, down-turned wingtips with lights
  for(const sd of [-1,1]){ edgeStrip(K,[sd*.57,.76],[sd*1.24,-.43],sd,.47,A); edgeStrip(K,[sd*.30,1.32],[sd*.53,.82],sd,.47,A);
    box(.03,.17,.27,M.shell2,sd*1.265,.40,-.575); box(.032,.012,.27,M.amberPaint,sd*1.266,.32,-.575); light(A,sd*1.27,.40,-.43,.016,g,.26); }
  // fans in the wing: duct lining, lit lip (white front pair, amber rear), nine-blade fan, stators, louvres
  for(const [x,z,r] of FANS){ const fg=new T.Group(); fg.position.set(x,.38,z); g.add(fg);
    const l=new T.Mesh(new T.CylinderGeometry(r+.02,r+.02,.165,40,1,true),M.duct); l.position.y=.0825; fg.add(l);
    const t=new T.Mesh(new T.TorusGeometry(r+.024,.007,6,48),on?(z>0?M.white:M.amber):M.amberDim); t.rotation.x=Math.PI/2; t.position.y=.162; fg.add(t);
    fanCore(fg,r,.16,{...o,vane:o.cruise?.5:0},x); }
  // spine: stepped body, glass sensor nose, lit dorsal line, blade aerial, cooling slats, battery hatch with gauge
  const SP=[[0,1.62],[.15,1.30],[.16,.60],[.14,-.05],[.18,-1.02],[-.18,-1.02],[-.14,-.05],[-.16,.60],[-.15,1.30]];
  plan(SP,.10,M.shell,.545);
  plan([[.12,.96],[.13,.60],[.11,-.05],[.145,-.95],[-.145,-.95],[-.11,-.05],[-.13,.60],[-.12,.96]],.06,M.shell2,.645);
  plan([[0,1.50],[.10,1.27],[.115,.965],[-.115,.965],[-.10,1.27]],.05,M.lens,.645);
  plan([[.07,.90],[.07,-.85],[-.07,-.85],[-.07,.90]],.02,M.shell,.705);
  box(.016,.006,1.5,A,0,.727,.02).castShadow=false;
  box(.012,.09,.10,M.shell2,0,.77,-.30,-.3,0,0);
  for(const sd of [-1,1]) for(let k=0;k<6;k++) box(.006,.04,.016,M.black,sd*.143,.675,-.40-k*.06);
  box(.12,.004,.24,M.black,0,.7255,-.62); for(let k=0;k<5;k++) box(.014,.006,.02,k<(o.charge??5)?M.white:M.black,-.04+k*.02,.7265,-.55);
  // twin canted fins: amber tip, white strobe, stencil
  for(const sd of [-1,1]){ const f=new T.Shape([[-1.06,0],[-.52,0],[-.82,.37],[-1.04,.39]].map(q=>new T.Vector2(q[0],q[1]))); const fgeo=new T.ExtrudeGeometry(f,{depth:.03,bevelEnabled:false}); fgeo.rotateY(-Math.PI/2); fgeo.translate(.015,0,0);
    const fm=new T.Mesh(fgeo,M.shell); fm.position.set(sd*.17,.64,0); fm.rotation.z=-sd*.38; fm.castShadow=true; g.add(fm); const FK=kit(fm);
    FK.box(.034,.02,.20,M.amberPaint,0,.38,-.93); FK.light(M.white,0,.40,-1.0,.012,fm,.2);
    const st=new T.Mesh(new T.PlaneGeometry(.20,.06),stencil('FR-T4')); st.position.set(sd*.017,.16,-.82); st.rotation.y=sd*Math.PI/2; fm.add(st); }
  // nose light, EO/IR turret under the nose, belly landing light
  light(M.white,0,.47,1.655,.016);
  box(.08,.03,.08,M.anod,0,.375,1.18); add(new T.SphereGeometry(.06,14,10),M.shell2,0,.33,1.18); cyl(.03,.014,12,M.lens,0,.33,1.235,Math.PI/2);
  // conformal belly cargo pod: doors swing down to lower a cradle of four crates on cables
  const POD=[[.22,.74],[.27,.56],[.27,-.56],[.20,-.72],[-.20,-.72],[-.27,-.56],[-.27,.56],[-.22,.74]];
  plan(POD,.23,M.shell2,.16); plan(scl(POD,.92),.03,M.shell,.13); box(.004,.006,1.2,M.black,0,.127,0);
  for(const sd of [-1,1]) box(.004,.012,1.1,A,sd*.272,.30,0).castShadow=false;
  light(M.white,0,.128,.62,.014,g,.18);
  const open=o.cargo==='winch', d=open?(o.drop??.5):0;
  if(open){ for(const sd of [-1,1]){ const hp=new T.Group(); hp.position.set(sd*.245,.13,0); hp.rotation.z=sd*1.35; g.add(hp); kit(hp).box(.24,.012,1.08,M.shell,-sd*.12,0,0); }
    for(const x of [-.20,.20]) for(const z of [-.42,.42]) tube(V(x,.15,z),V(x,.10-d,z),.006,M.steel,6);
    const cr=new T.Group(); cr.position.y=-d; g.add(cr); const CK=kit(cr); CK.box(.46,.03,.96,M.anod,0,.10,0); CK.box(.47,.006,.97,A,0,.085,0);
    for(const x of [-.115,.115]) for(const z of [-.22,.22]) crateBox(CK,.22,.22,.40,x,.226,z); }
  // tricycle gear with doors (drawn up in flight)
  if(!o.gearUp){ for(const [x,z] of [[0,1.02],[.42,-.66],[-.42,-.66]]){ tube(V(x,.40,z),V(x,.07,z),.022,M.steel,8); tube(V(x,.40,z),V(x,.20,z),.032,M.anod,8);
      cyl(.065,.05,14,M.rubber,x,.065,z,0,0,Math.PI/2); cyl(.035,.054,10,M.anod,x,.065,z,0,0,Math.PI/2); box(.01,.22,.18,M.shell2,x+(x>=0?.06:-.06),.28,z); } }
  return g; }

// ===================================================================== T5 CONCEPTS (medium, tilting rotors, cargo hold with a rear ramp)
// lathe body along z: pts [[z,r],...], octagonal like the in-game model can draw, squashed by sx/sy
function fus(K,pts,sx,sy,y,mat,segs=8,phi0=Math.PI/8,phiLen=TAU){ const ge=new T.LatheGeometry(pts.map(q=>new T.Vector2(q[1],q[0])),segs,phi0,phiLen); ge.rotateX(Math.PI/2); ge.scale(sx,sy,1); ge.translate(0,y,0); return K.add(ge,mat,0,0,0); }
function rotorHead(par,R,n,o,k,dir=1){ const K=kit(par); K.cyl(.05,.08,10,M.anod,0,.0,0); K.cyl(.06,.05,10,M.shell2,0,.06,0,0,0,0,par,.02);
  const p=new T.Group(); p.position.y=.03; par.add(p); prop2(p,k,R,o.spin,dir,(o.phase??0)+k,n,R/.3*.85); return p; }
// cargo hold behind a rear ramp: dark hold, crates, ramp hinged at the bottom
function hold(K,z0,w,h,y0,open,o){ const g=new T.Group(); K.add(new T.BoxGeometry(.001,.001,.001),M.black,0,0,0);
  if(open){ const hb=K.box(w-.06,h-.06,.9,M.black,0,y0+h/2,z0+.48); hb.castShadow=false; for(const x of [-.15,.15]) crateBox(K,.26,.26,.30,x,y0+.16,z0+.35); }
  const rg=new T.Group(); rg.position.set(0,y0,z0); rg.rotation.x=open?1.2:0; K.add(new T.BoxGeometry(.001,.001,.001),M.black,0,0,0); return rg; }

// A  TILT-WING CARRIER: the whole high wing pivots with four props on it (up for hover, flat for cruise),
//    T-tail with a pitch fan in the tailplane, cargo hold and rear ramp (real basis: Canadair CL-84, Airbus Vahana).
function t5a(o){
  const g=new T.Group(), K=kit(g), {box,cyl,tube,plan,light}=K, on=o.on!==false, A=on?M.amber:M.amberDim;
  const F=[[-1.72,.10],[-1.55,.26],[-1.2,.40],[.5,.44],[1.15,.40],[1.55,.26],[1.78,.06]];
  fus(K,F,1.0,.88,.66,M.shell); fus(K,[[1.12,.415],[1.5,.285],[1.70,.15]],1.01,.89,.66,M.lens);
  for(const sd of [-1,1]){ box(.006,.016,2.4,A,sd*.445,.62,-.05).castShadow=false; box(.006,.10,.40,M.batt,sd*.44,.74,.55); }
  // sponsons with the main wheels, nose wheel
  for(const sd of [-1,1]){ plan([[sd*.30,.30],[sd*.58,.20],[sd*.58,-.50],[sd*.30,-.60]],.18,M.shell2,.26); if(!o.gearUp) cyl(.10,.07,14,M.rubber,sd*.50,.10,-.15,0,0,Math.PI/2); }
  if(!o.gearUp){ tube(V(0,.32,1.25),V(0,.10,1.25),.025,M.steel,8); cyl(.08,.06,12,M.rubber,0,.08,1.25,0,0,Math.PI/2); }
  // rear ramp (closed: seam; open: down with the hold and crates showing)
  const open=o.cargo==='winch'; if(open){ const hb=box(.62,.48,.9,M.black,0,.52,-1.05); hb.castShadow=false; for(const x of [-.15,.15]) crateBox(K,.26,.26,.30,x,.42,-1.05); 
    const rp=new T.Group(); rp.position.set(0,.30,-1.35); rp.rotation.x=-1.0; g.add(rp); kit(rp).box(.62,.03,.72,M.shell2,0,0,-.36); } else box(.64,.005,.005,M.black,0,.30,-1.36);
  // T-tail with a pitch fan in the tailplane
  plan([[.03,-1.15],[.03,-1.70],[-.03,-1.70],[-.03,-1.15]],.01,M.shell,1.0);
  const fin=new T.Shape([[-1.15,0],[-1.72,0],[-1.80,.62],[-1.55,.62]].map(q=>new T.Vector2(q[0],q[1]))); const fg=new T.ExtrudeGeometry(fin,{depth:.05,bevelEnabled:false}); fg.rotateY(-Math.PI/2); fg.translate(.025,.95,0); K.add(fg,M.shell,0,0,0);
  slab(K,[[.70,-1.40],[.70,-1.78],[-.70,-1.78],[-.70,-1.40]],[[0,-1.59,.14]],1.56,.06,M.shell);
  { const pg=new T.Group(); pg.position.set(0,1.56,-1.59); g.add(pg); fanCore(pg,.13,.06,o,3); } light(M.white,0,1.65,-1.72,.014);
  // tilting wing: pylon, wing, four nacelles with three-blade props, tip lights
  box(.30,.30,.50,M.shell2,0,1.06,.15);
  const tilt=(o.cruise||!o.spin)?0:-1.45; const wg=new T.Group(); wg.position.set(0,1.18,.15); wg.rotation.x=tilt; g.add(wg); const WK=kit(wg);
  WK.box(4.4,.08,.60,M.shell,0,0,-.05); WK.box(4.42,.012,.02,A,0,.0,.255).castShadow=false; WK.cyl(.07,.36,10,M.steel,0,0,0,0,0,Math.PI/2);
  for(const x of [-2.0,-1.0,1.0,2.0]){ WK.box(.13,.13,.62,M.shell2,x,0,.18); const pr=new T.Group(); pr.position.set(x,0,.50); pr.rotation.x=Math.PI/2; wg.add(pr); rotorHead(pr,.46,3,o,x,x>0?1:-1);
    const ng=WK.cyl(.068,.02,12,on?(x>0?M.white:M.amber):M.amberDim,x,0,.47,Math.PI/2); }
  for(const sd of [-1,1]) WK.light(A,sd*2.21,0,-.05,.016,wg,.26);
  light(M.white,0,.70,1.79,.016);
  return g; }

// B  BOX-WING TILTROTOR: a low front wing and a high rear wing joined at the tips, a big three-blade rotor
//    on a tilting nacelle at each joint, slim fuselage with a hold and ramp (real basis: Bell V-280, Lockheed box wing).
function t5b(o){
  const g=new T.Group(), K=kit(g), {box,cyl,tube,plan,light}=K, on=o.on!==false, A=on?M.amber:M.amberDim;
  const F=[[-1.75,.08],[-1.5,.24],[-1.0,.36],[.6,.38],[1.2,.32],[1.6,.18],[1.78,.05]];
  fus(K,F,1.0,1.0,.62,M.shell); fus(K,[[1.15,.335],[1.52,.205],[1.70,.10]],1.01,1.01,.62,M.lens);
  for(const sd of [-1,1]) box(.006,.016,2.2,A,sd*.375,.62,-.05).castShadow=false;
  // front wing low, rear wing high on a dorsal fin, vertical tip joints
  plan([[.2,.85],[1.55,.10],[1.55,-.20],[.2,.30],[-.2,.30],[-1.55,-.20],[-1.55,.10],[-.2,.85]],.06,M.shell,.40);
  const fin=new T.Shape([[-.60,0],[-1.70,0],[-1.70,.78],[-1.30,.78]].map(q=>new T.Vector2(q[0],q[1]))); const fg=new T.ExtrudeGeometry(fin,{depth:.06,bevelEnabled:false}); fg.rotateY(-Math.PI/2); fg.translate(.03,.85,0); K.add(fg,M.shell,0,0,0);
  plan([[.2,-1.30],[1.55,-.30],[1.55,-.58],[.2,-1.70],[-.2,-1.70],[-1.55,-.58],[-1.55,-.30],[-.2,-1.30]],.06,M.shell,1.60);
  for(const sd of [-1,1]){ box(.06,1.26,.34,M.shell2,sd*1.58,1.0,-.30); box(.065,.012,.34,A,sd*1.58,1.66,-.30).castShadow=false;
    edgeStrip(K,[sd*.22,.85],[sd*1.54,.11],sd,.43,A,.012);
    const nv=new T.Group(); nv.position.set(sd*1.70,1.05,-.25); nv.rotation.x=o.cruise?Math.PI/2:0; g.add(nv); const NK=kit(nv);
    NK.cyl(.13,.70,10,M.shell,0,.15,0); NK.cyl(.135,.04,10,on?M.white:M.amberDim,0,.42,0); NK.cyl(.09,.15,10,M.anod,0,-.25,0,0,0,0,nv,.13);
    const pr=new T.Group(); pr.position.y=.52; nv.add(pr); rotorHead(pr,.84,3,o,sd,sd); }
  // hold and ramp, belly turret, skids
  const open=o.cargo==='winch'; if(open){ const hb=box(.54,.44,.9,M.black,0,.52,-1.0); hb.castShadow=false; for(const x of [-.13,.13]) crateBox(K,.24,.24,.30,x,.42,-1.0);
    const rp=new T.Group(); rp.position.set(0,.30,-1.30); rp.rotation.x=-1.0; g.add(rp); kit(rp).box(.54,.03,.66,M.shell2,0,0,-.33); } else box(.56,.005,.005,M.black,0,.30,-1.31);
  K.add(new T.SphereGeometry(.07,12,8),M.shell2,0,.25,1.0);
  if(!o.gearUp) for(const sd of [-1,1]){ tube(V(sd*.38,.03,-.9),V(sd*.38,.03,.9),.025,M.carbon,8); for(const z of [-.6,.6]) tube(V(sd*.2,.36,z),V(sd*.38,.04,z),.025,M.carbon,8); }
  light(M.white,0,.64,1.79,.016); light(M.amber,0,1.675,-1.50,.014);
  return g; }

// C  SIX-ROTOR TILT CARRIER: a broad blended body, a wing with four tilting nacelles and a V-tail with two more,
//    five-blade props, hold and ramp (real basis: Joby S4).
function t5c(o){
  const g=new T.Group(), K=kit(g), {box,cyl,tube,plan,light}=K, on=o.on!==false, A=on?M.amber:M.amberDim;
  const F=[[-1.80,.10],[-1.5,.30],[-.9,.46],[.5,.48],[1.1,.42],[1.5,.28],[1.75,.08]];
  fus(K,F,1.25,.78,.62,M.shell); fus(K,[[1.05,.43],[1.45,.29],[1.66,.14]],1.26,.79,.62,M.lens);
  for(const sd of [-1,1]) box(.006,.016,2.6,A,sd*.60,.60,-.1).castShadow=false;
  plan([[.3,.55],[1.95,.30],[1.95,.02],[.3,-.20],[-.3,-.20],[-1.95,.02],[-1.95,.30],[-.3,.55]],.06,M.shell,.92);
  for(const sd of [-1,1]){ const vt=new T.Group(); vt.position.set(sd*.10,.85,-1.45); vt.rotation.z=-sd*.75; g.add(vt); kit(vt).box(.05,1.1,.40,M.shell,0,.55,0); }
  const NAC=[[1.0,.92,.62],[-1.0,.92,.62],[1.95,.92,.18],[-1.95,.92,.18],[.88,1.66,-1.45],[-.88,1.66,-1.45]];
  NAC.forEach(([x,y,z],i)=>{ const nv=new T.Group(); nv.position.set(x,y,z); nv.rotation.x=o.cruise?Math.PI/2:0; g.add(nv); const NK=kit(nv);
    NK.cyl(.08,.40,10,M.shell,0,.10,0); NK.cyl(.083,.03,10,on?(z>0?M.white:M.amber):M.amberDim,0,.28,0); NK.cyl(.05,.12,10,M.anod,0,-.14,0,0,0,0,nv,.08);
    const pr=new T.Group(); pr.position.y=.33; nv.add(pr); rotorHead(pr,.42,5,o,i,x>0?1:-1); });
  for(const sd of [-1,1]) light(A,sd*1.96,.95,.0,.016);
  const open=o.cargo==='winch'; if(open){ const hb=box(.70,.40,.9,M.black,0,.50,-1.05); hb.castShadow=false; for(const x of [-.17,.17]) crateBox(K,.26,.26,.30,x,.42,-1.05);
    const rp=new T.Group(); rp.position.set(0,.30,-1.40); rp.rotation.x=-1.0; g.add(rp); kit(rp).box(.72,.03,.66,M.shell2,0,0,-.33); } else box(.74,.005,.005,M.black,0,.30,-1.41);
  if(!o.gearUp) for(const [x,z] of [[0,1.2],[.45,-.4],[-.45,-.4]]){ tube(V(x,.30,z),V(x,.09,z),.024,M.steel,8); cyl(.08,.06,12,M.rubber,x,.08,z,0,0,Math.PI/2); }
  light(M.white,0,.62,1.76,.016);
  return g; }

// ===================================================================== T5 TILTROTOR CARRIER (Justin picked concept C)
// A broad blended body with a glass nose, a wing with four tilting nacelles and a V-tail with two more, five-blade props
// with amber tips, flaperons and ruddervators, a satcom dome, EO/IR turret, a cargo hold with a rear ramp for eight
// blocks of crates, wheeled gear that retracts. o.cruise 0..1 tilts every nacelle from straight up to straight ahead.
function nacelle(par,o,lit,k,dir){ const K=kit(par);
  const pts=[[0,-.26],[.035,-.24],[.07,-.17],[.088,-.02],[.084,.16],[.07,.25],[0,.25]].map(q=>new T.Vector2(q[0],q[1]));
  const ge=new T.LatheGeometry(pts,10,Math.PI/10); K.add(ge,M.shell,0,0,0);
  const r=K.cyl(.089,.022,10,lit,0,.20,0); r.castShadow=false; K.cyl(.09,.01,10,M.black,0,.06,0);
  for(let i=0;i<4;i++) K.box(.006,.10,.012,M.black,Math.cos(i*Math.PI/2+.4)*.087,-.10,Math.sin(i*Math.PI/2+.4)*.087,0,-(i*Math.PI/2+.4),0);
  K.cyl(.05,.05,10,M.anod,0,.27,0); K.cyl(.05,.08,10,M.shell2,0,.33,0,0,0,0,par,.012);
  const p=new T.Group(); p.position.y=.31; par.add(p); prop2(p,k,.42,o.spin,dir,(o.phase??0)+k*1.3,5,1.2); return p; }
function t5(o){
  const g=new T.Group(), K=kit(g), {box,cyl,tube,plan,light,add}=K, on=o.on!==false, A=on?M.amber:M.amberDim;
  const tilt=(o.cruise||0)*Math.PI/2;
  const F=[[-1.80,.10],[-1.5,.30],[-.9,.46],[.5,.48],[1.1,.42],[1.5,.28],[1.75,.08]];
  const open=o.cargo==='winch';
  if(!open) fus(K,F,1.25,.78,.62,M.shell);
  else { fus(K,[[-.62,.455],[.5,.48],[1.1,.42],[1.5,.28],[1.75,.08]],1.25,.78,.62,M.shell); fus(K,[[-1.80,.10],[-1.5,.30],[-.9,.46],[-.62,.455]],1.25,.78,.62,M.duct,5,3*Math.PI/8,5*Math.PI/4);
    fus(K,[[-1.80,.0],[-1.79,.10]],1.25,.78,.62,M.shell); box(.44,.02,.80,M.shell2,0,.36,-1.05); }
  fus(K,[[1.03,.448],[1.45,.312],[1.68,.16]],1.25,.78,.62,M.lens);
  fus(K,[[.98,.452],[1.03,.452]],1.25,.78,.62,M.shell2);
  for(const sd of [-1,1]){ box(.006,.016,1.2,A,sd*.537,.58,-.2).castShadow=false;
    box(.006,.07,.22,M.batt,sd*.553,.66,.25); for(let k=0;k<5;k++) box(.004,.008,.03,k<(o.charge??5)?M.white:M.black,sd*.557,.68,.17+k*.04);
    const st=new T.Mesh(new T.PlaneGeometry(.20,.055),stencil('TC-T5')); st.position.set(sd*.49,.64,-.75); st.rotation.y=sd*Math.PI/2; g.add(st); }
  // wing: fairing, aerofoil slab with a rounded lit leading edge, flaperons; tip lights
  plan([[.42,.62],[.46,.0],[.40,-.34],[-.40,-.34],[-.46,.0],[-.42,.62]],.06,M.shell,.88);
  for(const sd of [-1,1]){ plan([[sd*.30,.55],[sd*1.95,.30],[sd*1.95,.10],[sd*.30,-.12]],.05,M.shell,.92);
    const a=V(sd*.30,.945,.55), b=V(sd*1.95,.945,.30); tube(a,b,.026,M.shell,10); edgeStrip(K,[sd*.30,.575],[sd*1.95,.325],sd>0?-1:1,.945,A,.012);
    plan([[sd*.40,-.14],[sd*1.90,.085],[sd*1.90,.035],[sd*.40,-.19]],.03,M.shell2,.925);
    light(A,sd*1.975,.945,.20,.016,g,.26); }
  // V-tail: two surfaces with ruddervators, a nacelle on each tip
  for(const sd of [-1,1]){ const vt=new T.Group(); vt.position.set(sd*.10,.85,-1.45); vt.rotation.z=-sd*.75; g.add(vt); const VK=kit(vt);
    VK.box(.05,1.10,.40,M.shell,0,.55,.02); VK.box(.03,1.0,.12,M.shell2,0,.55,-.25); VK.tube(V(0,0,.22),V(0,1.1,.22),.025,M.shell,8); }
  light(M.white,0,.99,-1.3,.014);
  // satcom dome, blade aerial, EO/IR turret, belly landing light
  { const d=add(new T.SphereGeometry(.14,14,8,0,TAU,0,Math.PI/2),M.grey,0,.975,-.55); d.scale.set(1,.45,1.3); }
  box(.012,.10,.12,M.shell2,0,1.02,-.95,-.35,0,0);
  box(.08,.03,.08,M.anod,0,.42,1.36); add(new T.SphereGeometry(.065,14,10),M.shell2,0,.37,1.36); cyl(.032,.014,12,M.lens,0,.37,1.42,Math.PI/2);
  light(M.white,0,.255,.70,.014,g,.18);
  // six tilting nacelles: pivot pylons on the wing, nacelles on the tips of wing and V-tail
  const NAC=[[1.0,.95,.66,1],[-1.0,.95,.66,1],[1.95,.95,.20,0],[-1.95,.95,.20,0],[.88,1.66,-1.45,0],[-.88,1.66,-1.45,0]];
  NAC.forEach(([x,y,z,pyl],i)=>{ if(pyl){ box(.07,.07,.30,M.shell2,x,.95,.50); cyl(.03,.12,8,M.steel,x,.95,.66,0,0,Math.PI/2); }
    else cyl(.03,.20,8,M.steel,x,y,z,0,0,Math.PI/2);
    const nv=new T.Group(); nv.position.set(x,y,z); nv.rotation.x=tilt; g.add(nv); nacelle(nv,o,on?(z>0?M.white:M.amber):M.amberDim,i,x>0?1:-1); });
  // cargo hold and rear ramp: closed shows the seam; open drops the ramp and shows four crates
  if(open){ for(const x of [-.13,.13]) for(const z of [-.85,-1.17]) crateBox(K,.24,.24,.28,x,.49,z); box(.46,.012,.012,A,0,.62,-.64).castShadow=false;
    const rp=new T.Group(); rp.position.set(0,.31,-.64); rp.rotation.x=-.85; g.add(rp); const RK=kit(rp); RK.box(.44,.025,.70,M.shell2,0,0,-.35); for(let k=0;k<4;k++) RK.box(.40,.006,.03,M.amberPaint,0,.016,-.12-k*.15); }
  // tricycle gear with wheel doors (drawn up in flight)
  if(!o.gearUp) for(const [x,z] of [[0,1.0],[.45,-.40],[-.45,-.40]]){ tube(V(x,.32,z),V(x,.10,z),.024,M.steel,8); tube(V(x,.32,z),V(x,.20,z),.034,M.anod,8);
    cyl(.085,.065,14,M.rubber,x,.085,z,0,0,Math.PI/2); cyl(.045,.07,10,M.anod,x,.085,z,0,0,Math.PI/2); box(.01,.20,.20,M.shell2,x+(x>=0?.07:-.07),.27,z); }
  light(M.white,0,.64,1.765,.016);
  return g; }

// ===================================================================== T6 CONCEPTS (medium, heavy freight: 16 blocks)
function rotor(par,R,n,o,k,dir,chord){ const K=kit(par); K.cyl(.09,.10,10,M.anod,0,0,0); K.cyl(.11,.06,10,M.shell2,0,.07,0,0,0,0,par,.04);
  const p=new T.Group(); p.position.y=.04; par.add(p); prop2(p,k,R,o.spin,dir,o.spin?(o.phase??0)+k:(k%2?Math.PI/2:0),n,chord); return p; }
function bigCrate(K,w,h,d,x,y,z){ const c=crateBox(K,w,h,d,x,y,z); for(const sx of [-1,1]) for(const sz of [-1,1]) K.box(.06,h+.01,.06,M.anod,x+sx*(w/2-.02),y,z+sz*(d/2-.02)); return c; }

// A  TANDEM ROTOR FREIGHTER: two big three-blade rotors fore and aft on pylons (the rear one higher so they overlap),
//    a long faceted hull with a glass nose, sponsons, a rear ramp and a winch hook under the belly (real basis: CH-47).
function r1t6a(o){
  const g=new T.Group(), K=kit(g), {box,cyl,tube,plan,light}=K, on=o.on!==false, A=on?M.amber:M.amberDim;
  const F=[[-1.72,.20],[-1.55,.42],[-1.2,.52],[1.0,.52],[1.35,.46],[1.6,.30],[1.72,.10]];
  fus(K,F,1.0,1.05,.82,M.shell); fus(K,[[1.25,.49],[1.55,.34],[1.70,.16]],1.0,1.05,.82,M.lens);
  for(const sd of [-1,1]){ box(.006,.016,2.4,A,sd*.485,.80,-.1).castShadow=false; plan([[sd*.40,.75],[sd*.68,.62],[sd*.68,-.70],[sd*.40,-.85]],.26,M.shell2,.30); box(.006,.012,1.3,A,sd*.682,.50,-.05).castShadow=false; }
  // pylons and rotors
  box(.42,.55,.60,M.shell,0,1.55,1.0); box(.44,.012,.62,A,0,1.83,1.0).castShadow=false; box(.40,.95,.80,M.shell,0,1.70,-1.05); box(.42,.012,.82,A,0,2.18,-1.05).castShadow=false;
  for(let k=0;k<5;k++) box(.006,.18,.03,M.black,.212,1.60,.85+k*.07);
  { const r=new T.Group(); r.position.set(0,1.86,1.0); g.add(r); rotor(r,1.25,3,o,1,1,2.6); }
  { const r=new T.Group(); r.position.set(0,2.22,-1.05); g.add(r); rotor(r,1.25,3,o,2,-1,2.6); }
  // rear ramp seam, winch hook and a 16-block container
  box(.70,.006,.006,M.black,0,.38,-1.50);
  box(.20,.06,.20,M.anod,0,.30,0); const d=o.cargo==='winch'?(o.drop??.4):0; tube(V(0,.28,0),V(0,.12-d,0),.008,M.tether,6);
  if(o.cargo==='winch'){ const cg=new T.Group(); cg.position.y=-d; g.add(cg); const CK=kit(cg); for(const x of [-.3,.3]) for(const z of [-.4,.4]) tube(V(0,.12,0),V(x,-.12,z),.005,M.tether,4); bigCrate(CK,.80,.55,1.10,0,-.40,0); }
  if(!o.gearUp) for(const [x,z] of [[.55,.45],[-.55,.45],[.55,-.55],[-.55,-.55]]){ tube(V(x,.32,z),V(x,.10,z),.026,M.steel,8); cyl(.10,.08,14,M.rubber,x,.10,z,0,0,Math.PI/2); }
  light(M.white,0,.82,1.73,.016); light(A,0,2.70,-1.05,.016); light(M.white,0,.27,1.1,.014,g,.18);
  return g; }

// B  INTERMESHING SKY CRANE: a slim spine on four tall legs, two intermeshing two-blade rotors on outward-leaning
//    masts, a sensor head forward and a finned tail boom; it straddles a 16-block container and clamps it (real basis: K-MAX, S-64).
function r1t6b(o){
  const g=new T.Group(), K=kit(g), {box,cyl,tube,plan,light,add}=K, on=o.on!==false, A=on?M.amber:M.amberDim;
  fus(K,[[-1.0,.30],[-.4,.36],[.9,.36],[1.25,.30],[1.45,.12]],1.0,1.0,1.85,M.shell);
  fus(K,[[1.12,.335],[1.38,.20],[1.50,.10]],1.0,1.0,1.85,M.lens);
  fus(K,[[-2.25,.06],[-1.9,.10],[-1.0,.22]],1.0,1.0,1.92,M.shell);
  { const f=new T.Shape([[-1.85,0],[-2.25,0],[-2.30,.62],[-2.05,.62]].map(q=>new T.Vector2(q[0],q[1]))); const fg=new T.ExtrudeGeometry(f,{depth:.05,bevelEnabled:false}); fg.rotateY(-Math.PI/2); fg.translate(.025,1.92,0); K.add(fg,M.shell,0,0,0); }
  box(.80,.04,.22,M.shell,0,1.95,-2.05); light(M.white,0,2.56,-2.18,.014);
  for(const sd of [-1,1]){ box(.006,.016,1.6,A,sd*.335,1.85,.15).castShadow=false;
    const m=new T.Group(); m.position.set(sd*.16,2.15,.10); m.rotation.z=-sd*.22; g.add(m); const MK=kit(m); MK.cyl(.10,.40,10,M.shell2,0,.12,0); MK.cyl(.105,.012,10,A,0,.30,0);
    const r=new T.Group(); r.position.y=.34; m.add(r); rotor(r,1.55,2,{...o,phase:(o.phase??0)+(sd>0?0:Math.PI/2)},sd,sd,2.8); }
  box(.30,.12,.50,M.shell2,0,2.12,.10);
  // legs, clamp arms and the container
  for(const [x,z] of [[1,1],[1,-1],[-1,1],[-1,-1]]){ tube(V(x*.25,1.62,z*.55),V(x*.72,.04,z*.85),.04,M.shell2,8); box(.16,.04,.22,M.rubber,x*.72,.02,z*.85);
    tube(V(x*.25,1.62,z*.55),V(x*.50,1.45,z*.62),.025,M.steel,6); }
  if(o.cargo){ const d=o.cargo==='winch'?(o.drop??0):0; const cg=new T.Group(); cg.position.y=-d; g.add(cg); const CK=kit(cg); bigCrate(CK,.90,.85,1.25,0,1.0,0);
    for(const [x,z] of [[1,1],[1,-1],[-1,1],[-1,-1]]){ CK.box(.04,.30,.04,M.anod,x*.47,1.33,z*.64); CK.box(.06,.04,.06,M.amberPaint,x*.47,1.47,z*.64); } }
  for(const [x,z] of [[1,1],[1,-1],[-1,1],[-1,-1]]) tube(V(x*.12,1.55,z*.4),V(x*.47,1.47,z*.64),.02,M.anod,6);
  add(new T.SphereGeometry(.07,12,8),M.shell2,0,1.52,1.15); light(M.white,0,1.85,1.51,.016);
  return g; }

// C  COAXIAL COMPOUND FREIGHTER: two stacked four-blade rigid rotors on one mast, a six-blade pusher prop in a ring
//    at the tail, a sleek hull with sliding side doors on a 16-block hold, twin-fin tailplane (real basis: Sikorsky X2 / Raider).
function r1t6c(o){
  const g=new T.Group(), K=kit(g), {box,cyl,tube,plan,light,add}=K, on=o.on!==false, A=on?M.amber:M.amberDim;
  fus(K,[[-1.1,.30],[-.7,.46],[.8,.50],[1.25,.42],[1.55,.24],[1.72,.08]],1.0,1.0,.82,M.shell);
  fus(K,[[1.18,.445],[1.5,.27],[1.68,.12]],1.0,1.0,.82,M.lens);
  fus(K,[[-2.05,.14],[-1.6,.18],[-1.1,.30]],1.0,1.0,.90,M.shell);
  // mast fairing and the coaxial pair
  fus(K,[[-.35,.0],[-.2,.14],[.4,.16],[.65,.0]],1.0,1.2,1.32,M.shell);
  cyl(.08,.40,10,M.shell2,0,1.62,.15); cyl(.12,.10,12,M.shell,0,1.70,.15); cyl(.125,.012,12,A,0,1.70,.15).castShadow=false;
  for(const [y,dir,k] of [[1.55,1,0],[1.86,-1,1]]){ const r=new T.Group(); r.position.set(0,y,.15); g.add(r); rotor(r,1.42,4,{...o,phase:(o.phase??0)+k*Math.PI/4},k,dir,2.4); }
  // tail: tailplane with end fins, ringed pusher
  box(1.10,.05,.30,M.shell,0,.92,-1.75); for(const sd of [-1,1]){ box(.04,.40,.32,M.shell,sd*.56,1.05,-1.78); light(A,sd*.56,1.27,-1.78,.014); }
  { const pg=new T.Group(); pg.position.set(0,.90,-2.12); pg.rotation.x=Math.PI/2; g.add(pg); ductRing(pg,.38,.16,M.duct,A); const pk=new T.Group(); pk.position.y=.08; pg.add(pk); prop2(pk,0,.36,o.spin,1,o.phase??0,6,1.1); kit(pg).cyl(.06,.20,10,M.shell2,0,.08,0); }
  // sliding side doors on the hold (open in flight shows crates), stripes, gear
  const open=o.cargo==='winch';
  for(const sd of [-1,1]){ box(.006,.016,1.6,A,sd*.462,.82,.0).castShadow=false;
    if(open){ const hb=box(.01,.50,.80,M.black,sd*.466,.78,-.15); hb.castShadow=false; box(.02,.50,.80,M.shell2,sd*.48,.78,-.95);
      for(let k=0;k<2;k++) crateBox(K,.30,.30,.30,sd*.28,.70,-.32+k*.34); }
    else { box(.006,.50,.006,M.black,sd*.463,.78,.25); box(.006,.50,.006,M.black,sd*.463,.78,-.55); } }
  if(!o.gearUp) for(const [x,z] of [[0,1.15],[.42,-.45],[-.42,-.45]]){ tube(V(x,.36,z),V(x,.10,z),.026,M.steel,8); cyl(.09,.07,14,M.rubber,x,.09,z,0,0,Math.PI/2); }
  add(new T.SphereGeometry(.07,12,8),M.shell2,0,.34,1.35); light(M.white,0,.82,1.73,.016);
  return g; }

// (round-1 T6 concepts kept above for reference; round 2 replaces them)
// ===================================================================== T6 ROUND 2: HARD SCI-FI FREIGHTERS (16-block cargo module)
const modTex=ctex(256,256,(x,w,h)=>{ x.fillStyle='#3a4046'; x.fillRect(0,0,w,h); for(let i=0;i<w;i+=12){ x.fillStyle='#2c3135'; x.fillRect(i,0,4,h); x.fillStyle='#4a5157'; x.fillRect(i+4,0,2,h); }
  x.fillStyle='#d9962f'; x.fillRect(0,104,w,48); x.fillStyle='#16181a'; x.font='700 30px "DejaVu Sans Mono", monospace'; x.textAlign='center'; x.fillText('CARGO 16',w/2,139);
  x.strokeStyle='#1f2326'; x.lineWidth=10; x.strokeRect(5,5,w-10,h-10); });
const hazTex=ctex(64,64,(x,w,h)=>{ x.fillStyle='#1a1c1e'; x.fillRect(0,0,w,h); x.fillStyle='#d9962f'; for(let i=-64;i<128;i+=16){ x.beginPath(); x.moveTo(i,0); x.lineTo(i+8,0); x.lineTo(i+8-64,64); x.lineTo(i-64,64); x.fill(); } });
M.mod=std({map:modTex, roughness:.6, metalness:.3});
const modTex2=ctex(128,256,(x,w,h)=>{ x.fillStyle='#3a4046'; x.fillRect(0,0,w,h); for(let i=0;i<w;i+=12){ x.fillStyle='#2c3135'; x.fillRect(i,0,4,h); x.fillStyle='#4a5157'; x.fillRect(i+4,0,2,h); } x.fillStyle='#d9962f'; x.fillRect(0,104,w,48); });
M.mod2=std({map:modTex2, roughness:.6, metalness:.3}); M.haz=std({map:hazTex, roughness:.6});
M.armour=std({map:shellTex, color:0xffffff, roughness:.5, metalness:.35, flatShading:true});
// chamfered prism along z (w x h section, corners cut by c), centred
function cbox(K,w,h,d,c,mat,x,y,z,rx=0,ry=0,rz=0){ const a=w/2,b=h/2; const sh=new T.Shape([[-a+c,-b],[a-c,-b],[a,-b+c],[a,b-c],[a-c,b],[-a+c,b],[-a,b-c],[-a,-b+c]].map(q=>new T.Vector2(q[0],q[1])));
  const ge=new T.ExtrudeGeometry(sh,{depth:d,bevelEnabled:false}); ge.translate(0,0,-d/2); return K.add(ge,mat,x,y,z,rx,ry,rz); }
// cargo module: corrugated box, corner castings, lock points
function cargoModule(K,w,h,d,x,y,z){ K.box(w,h,d,M.mod,x,y,z); for(const sx of [-1,1]) for(const sy of [-1,1]) for(const sz of [-1,1]) K.box(.10,.10,.10,M.anod,x+sx*(w/2-.04),y+sy*(h/2-.04),z+sz*(d/2-.04));
  for(const sx of [-1,1]) K.box(.012,.06,d*.6,M.black,x+sx*(w/2+.002),y+h/2-.12,z); }
// armoured duct: octagonal armour shell, lined bore, lit lip, fan core; built round a group at its base
function armDuct(par,R,H,o,k,lit){ const K=kit(par);
  const sh=new T.Mesh(new T.CylinderGeometry(R+.10,R+.10,H,8,1,true),M.armour); sh.position.y=H/2; sh.rotation.y=Math.PI/8; sh.castShadow=true; par.add(sh);
  ring(K,0,H,0,R+.01,R+.11,.025,M.shell2).rotation.y=0; ring(K,0,-.025,0,R+.01,R+.11,.025,M.shell2);
  const ln=new T.Mesh(new T.CylinderGeometry(R+.012,R+.012,H,32,1,true),M.duct); ln.position.y=H/2; par.add(ln);
  const t=new T.Mesh(new T.TorusGeometry(R+.03,.009,6,48),lit); t.rotation.x=Math.PI/2; t.position.y=H+.03; par.add(t);
  for(let i=0;i<8;i++){ const a=i/8*TAU; K.box(.05,H*.6,.02,M.shell2,Math.sin(a)*(R+.105),H/2,Math.cos(a)*(R+.105),0,a,0); }
  fanCore(par,R,H*.9,o,k); }

// A  FRAME HAULER: an armoured gantry frame wrapped over the cargo module, four clamp arms, an angular sensor head,
//    a battery block at the back, four ducts on outrigger booms that gimbal for thrust, four landing struts.
function t6a(o){
  const g=new T.Group(), K=kit(g), {box,cyl,tube,light,add}=K, on=o.on!==false, A=on?M.amber:M.amberDim, tilt=(o.cruise||0)*.5;
  // top deck and side rails
  cbox(K,1.75,.26,2.9,.08,M.armour,0,1.86,0); cbox(K,1.45,.10,2.5,.04,M.shell2,0,2.03,-.05);
  for(const z of [-.9,0,.9]) box(1.47,.004,.004,M.black,0,2.081,z); box(.02,.006,2.3,A,0,2.084,-.05).castShadow=false;
  for(const sd of [-1,1]){ cbox(K,.20,1.35,2.7,.05,M.armour,sd*.80,1.12,0); box(.006,.016,2.5,A,sd*.902,1.60,0).castShadow=false;
    for(const z of [-1.0,-.35,.35,1.0]) box(.006,1.2,.006,M.black,sd*.902,1.12,z);
    for(let k=0;k<6;k++) box(.008,.10,.03,M.black,sd*.903,.80,-.25+k*.10);
    tube(V(sd*.78,.55,1.32),V(sd*.78,1.70,1.32),.02,M.steel,6); }
  // sensor head: angular wedge with a visor slit and two sensor eyes
  const hd=new T.Group(); hd.position.set(0,1.72,1.45); g.add(hd); const HK=kit(hd);
  HK.prof([[0,-.25],[.42,-.12],[.52,.05],[.30,.30],[0,.30]],1.10,M.armour); HK.prof([[.33,.0],[.50,.04],[.36,.20],[.30,.20]],.80,M.lens);
  HK.box(.70,.02,.10,A,0,-.14,.42).castShadow=false; for(const sd of [-1,1]) HK.light(M.white,sd*.45,.10,.38,.022,hd,.3);
  hd.children.forEach(m=>{}); 
  // rear battery block with vents and gauge
  cbox(K,1.30,.55,.50,.06,M.armour,0,1.66,-1.55); for(let k=0;k<7;k++) box(1.0,.012,.012,M.black,0,1.48+k*.05,-1.802);
  for(let k=0;k<6;k++) box(.06,.03,.006,k<(o.charge??6)?M.white:M.black,-.18+k*.07,1.86,-1.803); light(A,-.55,1.75,-1.80,.018); light(A,.55,1.75,-1.80,.018);
  // outrigger booms and gimballed ducts
  for(const [sx,sz] of [[1,1],[-1,1],[1,-1],[-1,-1]]){ const a=V(sx*.82,1.88,sz*1.15), b=V(sx*1.85,1.90,sz*1.50);
    const bm=tube(a,b,.07,M.armour,8); tube(a.clone().add(V(0,-.12,0)),b.clone().add(V(0,-.18,0)),.025,M.steel,6);
    const dg=new T.Group(); dg.position.set(sx*2.0,1.70,sz*1.55); dg.rotation.x=tilt; g.add(dg); const DK=kit(dg);
    for(const s2 of [-1,1]) DK.box(.08,.30,.10,M.shell2,s2*.62,.20,0); armDuct(dg,.50,.40,o,sx+sz,on?(sz>0?M.white:M.amber):M.amberDim); }
  // clamp arms and the module
  for(const [sx,sz] of [[1,1],[-1,1],[1,-1],[-1,-1]]){ const top=V(sx*.55,1.74,sz*1.0); tube(top,V(sx*.66,1.30,sz*1.0),.035,M.steel,8); tube(top,V(sx*.62,1.48,sz*1.0),.05,M.anod,8);
    box(.06,.22,.20,M.haz,sx*.69,1.22,sz*1.0); }
  if(o.cargo){ const d=o.cargo==='winch'?(o.drop??0):0; const cg=new T.Group(); cg.position.y=-d; g.add(cg); cargoModule(kit(cg),1.30,.92,2.30,0,1.02,0); }
  // landing struts
  if(!o.gearUp) for(const [sx,sz] of [[1,1],[-1,1],[1,-1],[-1,-1]]){ tube(V(sx*.82,.50,sz*1.25),V(sx*1.0,.05,sz*1.45),.05,M.armour,8); box(.24,.05,.30,M.haz,sx*1.0,.025,sz*1.45); tube(V(sx*.85,.85,sz*1.2),V(sx*.97,.30,sz*1.42),.022,M.steel,6); }
  // mast and strobes
  tube(V(.5,2.08,-.9),V(.5,2.45,-.9),.012,M.anod,6); light(M.white,.5,2.46,-.9,.016); light(M.white,0,2.09,1.25,.014);
  return g; }

// B  TWIN-HULL CARRIER: two armoured hulls with three lift fans buried in each, a bridge deck over the cargo bay,
//    big gimballed ducts on stub wings, wedge noses with visors, canted tail fins; the module hangs between the hulls.
function t6b(o){
  const g=new T.Group(), K=kit(g), {box,cyl,tube,light,add}=K, on=o.on!==false, A=on?M.amber:M.amberDim, tilt=(o.cruise||0)*.5;
  const FZ=[1.15,0,-1.15];
  for(const sd of [-1,1]){ const hx=sd*1.02;
    const top=new T.Shape([[-.40,-1.85],[.40,-1.85],[.40,1.50],[.20,1.95],[-.20,1.95],[-.40,1.50]].map(q=>new T.Vector2(q[0],-q[1]))); for(const z of FZ){ const hp=new T.Path(); hp.absarc(0,-z,.33,0,TAU,true); top.holes.push(hp); }
    const ge=new T.ExtrudeGeometry(top,{depth:.50,bevelEnabled:false,curveSegments:24}); ge.rotateX(-Math.PI/2); K.add(ge,M.armour,hx,.85,0);
    cbox(K,.70,.20,3.4,.06,M.shell2,hx,.80,-.15);
    for(const z of FZ){ const fg=new T.Group(); fg.position.set(hx,.78,z); g.add(fg); const l=new T.Mesh(new T.CylinderGeometry(.33,.33,.57,32,1,true),M.duct); l.position.y=.285; fg.add(l);
      const t=new T.Mesh(new T.TorusGeometry(.345,.009,6,48),on?(z>0?M.white:M.amber):M.amberDim); t.rotation.x=Math.PI/2; t.position.y=.575; fg.add(t); fanCore(fg,.32,.55,o,z+sd); }
    box(.006,.016,3.3,A,sd*(1.02+.403)*1,1.18,-.15).castShadow=false; for(const z of [-.58,.58]) box(.006,.46,.006,M.black,sd*1.423,1.10,z);
    for(let k=0;k<5;k++) box(.007,.08,.03,M.black,sd*1.424,.98,1.35-k*.07);
    // wedge nose visor and canted fin
    const vz=V(hx,1.12,1.70); box(.42,.05,.02,M.lens,hx,1.20,1.952); light(M.white,hx,1.05,1.955,.02);
    const fin=new T.Shape([[-1.85,0],[-1.25,0],[-1.55,.48],[-1.85,.50]].map(q=>new T.Vector2(q[0],q[1]))); const fgeo=new T.ExtrudeGeometry(fin,{depth:.05,bevelEnabled:false}); fgeo.rotateY(-Math.PI/2); fgeo.translate(.025,0,0);
    const fm=new T.Mesh(fgeo,M.armour); fm.position.set(hx+sd*.25,1.35,0); fm.rotation.z=-sd*.3; fm.castShadow=true; g.add(fm); kit(fm).light(A,0,.50,-1.85,.016,fm,.24);
    // stub wing with a big gimballed duct
    cbox(K,.60,.12,.50,.03,M.armour,sd*1.66,1.10,0,0,Math.PI/2,0);
    const dg=new T.Group(); dg.position.set(sd*2.15,.92,0); dg.rotation.x=tilt; g.add(dg); armDuct(dg,.46,.36,o,sd*3,A);
    // legs
    if(!o.gearUp) for(const z of [-1.3,1.3]){ tube(V(hx,.72,z),V(hx+sd*.15,.05,z),.05,M.armour,8); box(.30,.05,.24,M.haz,hx+sd*.15,.025,z); } }
  // bridge decks fore and aft, spine with sensor mast; module between the hulls
  for(const z of [1.40,-1.45]){ cbox(K,1.40,.22,.55,.05,M.armour,0,1.24,z,0,0,0); box(1.2,.012,.012,A,0,1.13,z+(z>0?.28:-.28)).castShadow=false; }
  cbox(K,.30,.14,2.30,.03,M.shell2,0,1.30,0); tube(V(0,1.37,-1.2),V(0,1.75,-1.2),.012,M.anod,6); light(M.white,0,1.76,-1.2,.016);
  for(const [sx,sz] of [[1,1],[-1,1],[1,-1],[-1,-1]]){ tube(V(sx*.40,1.13,sz*1.25),V(sx*.55,.98,sz*1.05),.03,M.steel,6); box(.06,.18,.18,M.haz,sx*.585,.92,sz*1.0); }
  if(o.cargo){ const d=o.cargo==='winch'?(o.drop??0):0; const cg=new T.Group(); cg.position.y=-d; g.add(cg); cargoModule(kit(cg),1.10,.85,2.30,0,.73,0); }
  return g; }

// C  HEX LIFTER: a hexagonal armoured core with a stepped crown, six armoured ducts on radial pylons, a hexagonal
//    cargo pod docked under a clamp ring, three angular landing legs, sensor cluster forward.
function hexPts(r,rot=0){ const a=[]; for(let i=0;i<6;i++){ const t=rot+i*TAU/6; a.push([Math.sin(t)*r,Math.cos(t)*r]); } return a; }
function t6c(o){
  const g=new T.Group(), K=kit(g), {box,cyl,tube,plan,light,add}=K, on=o.on!==false, A=on?M.amber:M.amberDim, tilt=(o.cruise||0)*.45;
  plan(hexPts(.95),.45,M.armour,1.50); plan(hexPts(.80),.14,M.shell2,1.95); plan(hexPts(.55),.10,M.armour,2.09); plan(hexPts(.30),.06,M.shell2,2.19);
  const band=plan(hexPts(.958),.02,A,1.82); band.castShadow=false;
  for(let i=0;i<6;i++){ const t=i*TAU/6+TAU/12, x=Math.sin(t)*.83, z=Math.cos(t)*.83; for(let k=0;k<4;k++) box(.03,.10,.006,M.black,x+Math.cos(t)*(-.06+k*.04),1.65,z-Math.sin(t)*(-.06+k*.04),0,t,0); }
  // sensor cluster forward: visor wedge with three eyes
  const sg=new T.Group(); sg.position.set(0,1.70,.80); g.add(sg); const SK=kit(sg); SK.prof([[0,-.18],[.30,-.10],[.38,.05],[.25,.22],[0,.22]],.80,M.armour); SK.prof([[.27,.0],[.36,.04],[.27,.16],[.24,.16]],.60,M.lens);
  for(const x of [-.22,0,.22]) SK.light(x?M.white:A,x,-.06,.33,.02,sg,.26);
  tube(V(-.2,2.25,-.1),V(-.2,2.60,-.1),.012,M.anod,6); light(M.white,-.2,2.61,-.1,.016); add(new T.SphereGeometry(.12,12,6,0,TAU,0,Math.PI/2),M.grey,.2,2.25,-.1).scale.set(1,.5,1);
  // six radial pylons and ducts
  for(let i=0;i<6;i++){ const t=TAU/12+i*TAU/6, cx=Math.sin(t), cz=Math.cos(t); const a=V(cx*.85,1.72,cz*.85), b=V(cx*1.45,1.80,cz*1.45);
    const pg=new T.Group(); pg.position.copy(a); pg.rotation.y=t; g.add(pg); const PK=kit(pg); cbox(PK,.22,.18,.80,.04,M.armour,0,.06,.40); PK.box(.006,.012,.70,A,.112,.10,.40).castShadow=false;
    const dg=new T.Group(); dg.position.set(cx*1.9,1.62,cz*1.9); dg.rotation.set(tilt,0,0); g.add(dg); armDuct(dg,.46,.36,o,i,on?(cz>0?M.white:M.amber):M.amberDim); }
  // clamp ring and the hex cargo pod
  plan(hexPts(.78),.08,M.anod,1.42); for(let i=0;i<6;i++){ const t=i*TAU/6, x=Math.sin(t)*.70, z=Math.cos(t)*.70; box(.10,.16,.10,M.haz,x,1.34,z); }
  if(o.cargo){ const d=o.cargo==='winch'?(o.drop??0):0; const cg=new T.Group(); cg.position.y=-d; g.add(cg); const CK=kit(cg);
    const pod=CK.plan(hexPts(.66),.82,M.mod2,.52);
    for(let i=0;i<6;i++){ const t=i*TAU/6+TAU/12, ap=.66*Math.cos(Math.PI/6)+.003; const st=new T.Mesh(new T.PlaneGeometry(.42,.10),stencil(i%2?'16':'CARGO','#16181a',192,48,'700 34px "DejaVu Sans Mono", monospace')); st.position.set(Math.sin(t)*ap,.93,Math.cos(t)*ap); st.rotation.y=t; cg.add(st); } CK.plan(hexPts(.68),.06,M.anod,.50); CK.plan(hexPts(.68),.06,M.anod,1.30); }
  // three angular legs
  if(!o.gearUp) for(let i=0;i<3;i++){ const t=i*TAU/3+Math.PI, cx=Math.sin(t), cz=Math.cos(t); tube(V(cx*.80,1.52,cz*.80),V(cx*1.20,.05,cz*1.20),.06,M.armour,8); tube(V(cx*.75,1.0,cz*.75),V(cx*1.05,.6,cz*1.05),.025,M.steel,6); box(.32,.05,.32,M.haz,cx*1.2,.025,cz*1.2); }
  return g; }

// ===================================================================== T6 ROUND 3: TWELVE FREIGHTER OPTIONS
M.armourD=std({map:shellTex, color:0xffffff, roughness:.5, metalness:.35, side:T.DoubleSide});
function fanHole(g,x,y,z,r,h,lit,o,k=0){ const fg=new T.Group(); fg.position.set(x,y,z); g.add(fg); const l=new T.Mesh(new T.CylinderGeometry(r,r,h,32,1,true),M.duct); l.position.y=h/2; fg.add(l);
  const t=new T.Mesh(new T.TorusGeometry(r+.012,.008,6,48),lit); t.rotation.x=Math.PI/2; t.position.y=h; fg.add(t); fanCore(fg,r*.98,h*.95,o,k); return fg; }
function holedSlab(K,outline,holes,y,h,mat){ return slab(K,outline,holes,y,h,mat); }
function legStrut(K,a,b,pad=.26){ K.tube(a,b,.045,M.armour,8); K.tube(a.clone().lerp(b,.3),b.clone().add(V(0,.25,0)),.02,M.steel,6); K.box(pad,.05,pad,M.haz,b.x,.025,b.z); }
const LIT=(on,front)=>on?(front?M.white:M.amber):M.amberDim;

// 1 RING-WING: a slim fuselage inside a closed ring wing, four tilting nacelles on the ring (real basis: Lockheed ring-wing studies)
function o6_1(o){ const g=new T.Group(), K=kit(g), {box,cyl,tube,light}=K, on=o.on!==false, A=on?M.amber:M.amberDim, cy=1.35;
  fus(K,[[-1.7,.12],[-1.3,.36],[-.6,.44],[.9,.44],[1.4,.34],[1.75,.10]],1.0,1.0,cy,M.armour); fus(K,[[1.15,.42],[1.5,.27],[1.72,.13]],1.0,1.0,cy,M.lens);
  fus(K,[[-.75,1.12],[-.35,1.17],[.05,1.17],[.25,1.13]],1,1,cy,M.armourD,16,0); fus(K,[[-.70,1.10],[.20,1.105]],1,1,cy,M.shell2,16,0);
  for(let i=0;i<4;i++){ const a=Math.PI/4+i*Math.PI/2, x=Math.sin(a)*1.0, y=cy+Math.cos(a)*1.0; tube(V(0,cy,-.3),V(x,y,-.3),.05,M.armour,6); }
  for(let i=0;i<4;i++){ const a=Math.PI/4+i*Math.PI/2, x=Math.sin(a)*1.25, y=cy+Math.cos(a)*1.25; const nv=new T.Group(); nv.position.set(x,y,.15); nv.scale.setScalar(1.2); nv.rotation.x=(o.cruise||0)*Math.PI/2; g.add(nv); nacelle(nv,o,LIT(on,y>cy),i,x>0?1:-1); cyl(.04,.3,8,M.steel,x*.92,y-(y-cy)*.08,.15,0,0,Math.PI/2); }
  cargoModule(K,.80,.55,1.50,0,.70,-.25); for(const z of [-.85,.35]) for(const sx of [-1,1]) tube(V(sx*.3,1.0,z),V(sx*.38,.95,z),.03,M.steel,6);
  for(const z of [-.6,.1]) { box(.5,.06,.2,M.haz,0,.19,z); } box(.012,.012,1.0,A,0,1.80,-.2).castShadow=false; light(M.white,0,cy,1.76,.016); light(A,0,2.53,-.3,.016);
  return g; }

// 2 CYCLOROTOR: an armoured box hull swung between four paddle-wheel cyclorotors (real basis: CycloTech CruiseUAV)
function o6_2(o){ const g=new T.Group(), K=kit(g), {box,cyl,tube,light,prof}=K, on=o.on!==false, A=on?M.amber:M.amberDim;
  cbox(K,1.30,1.10,2.9,.12,M.armour,0,1.30,-.1); prof([[1.35,.75],[1.75,.95],[1.75,1.55],[1.35,1.85]],1.06,M.armour); prof([[1.62,1.25],[1.77,1.30],[1.77,1.50],[1.55,1.62]],.86,M.lens);
  box(1.0,.006,.006,M.black,0,1.85,-.6); for(const sd of [-1,1]){ box(.006,.70,1.20,M.shell2,sd*.652,1.25,-.55); const st=new T.Mesh(new T.PlaneGeometry(.5,.12),stencil('CARGO 16','#d59a4a',256,48)); st.position.set(sd*.66,1.25,-.55); st.rotation.y=sd*Math.PI/2; g.add(st); box(.006,.016,2.6,A,sd*.652,1.75,-.1).castShadow=false; }
  for(const [sx,z] of [[1,.85],[-1,.85],[1,-1.05],[-1,-1.05]]){ const dg=new T.Group(); dg.position.set(sx*1.15,1.30,z); g.add(dg); const DK=kit(dg);
    for(const ex of [-.42,.42]){ DK.cyl(.55,.04,16,M.armour,ex,0,0,0,0,Math.PI/2); DK.cyl(.56,.012,16,A,ex+(ex>0?.025:-.025),0,0,0,0,Math.PI/2); }
    DK.cyl(.06,.9,10,M.steel,0,0,0,0,0,Math.PI/2); for(let b=0;b<6;b++){ const a=b/6*TAU+(o.phase??0); const bl=DK.box(.84,.03,.16,M.prop,0,Math.sin(a)*.47,Math.cos(a)*.47); bl.rotation.x=-a+.3; }
    DK.box(.30,.20,.20,M.shell2,-sx*.55,0,0); }
  if(!o.gearUp) for(const z of [-1.2,1.0]) for(const sx of [-1,1]) legStrut(K,V(sx*.5,.80,z),V(sx*.62,.05,z*1.05));
  light(M.white,0,1.62,1.78,.016); light(A,0,1.62,-1.56,.016); return g; }

// 3 MANTIS: an angular thorax on four jointed legs, an armoured duct on every knee, the module slung under the belly
function o6_3(o){ const g=new T.Group(), K=kit(g), {box,cyl,tube,light,prof,add}=K, on=o.on!==false, A=on?M.amber:M.amberDim;
  cbox(K,1.0,.55,2.0,.12,M.armour,0,2.05,-.1); cbox(K,.8,.25,1.6,.08,M.shell2,0,2.42,-.2); box(.02,.006,1.4,A,0,2.552,-.2).castShadow=false;
  const hd=new T.Group(); hd.position.set(0,2.0,.9); g.add(hd); const HK=kit(hd); HK.prof([[0,-.25],[.35,-.12],[.45,.06],[.25,.26],[0,.28]],.70,M.armour); HK.prof([[.30,.0],[.44,.06],[.30,.18],[.26,.18]],.50,M.lens); for(const sx of [-1,1]) HK.light(M.white,sx*.30,.12,.34,.02,hd,.26);
  for(const [sx,sz] of [[1,1],[-1,1],[1,-1],[-1,-1]]){ const hip=V(sx*.45,2.0,sz*.65-.1), knee=V(sx*1.15,2.45,sz*1.05-.1), foot=V(sx*1.55,.05,sz*1.35-.1);
    add(new T.SphereGeometry(.12,10,8),M.shell2,hip.x,hip.y,hip.z); tube(hip,knee,.09,M.armour,6); tube(knee,foot,.07,M.armour,6); tube(hip.clone().add(V(0,-.15,0)),knee.clone().lerp(foot,.35),.025,M.steel,6);
    add(new T.SphereGeometry(.13,10,8),M.shell2,knee.x,knee.y,knee.z); box(.30,.06,.36,M.haz,foot.x,.03,foot.z);
    const dg=new T.Group(); dg.position.set(knee.x,knee.y+.12,knee.z); dg.rotation.x=(o.cruise||0)*.5; g.add(dg); armDuct(dg,.40,.32,o,sx+sz,LIT(on,sz>0)); }
  if(o.cargo){ cargoModule(K,1.05,.75,1.70,0,1.15,-.1); for(const [sx,sz] of [[1,1],[-1,1],[1,-1],[-1,-1]]) tube(V(sx*.38,1.78,sz*.70-.1),V(sx*.48,1.45,sz*.80-.1),.03,M.steel,6); }
  light(A,0,2.56,-.9,.016); return g; }

// 4 TWIN-BOOM: a cargo pod between two armoured booms, four ducts set in the booms, a wing and a tail bar with twin fins
function o6_4(o){ const g=new T.Group(), K=kit(g), {box,cyl,tube,light,prof}=K, on=o.on!==false, A=on?M.amber:M.amberDim;
  cbox(K,1.10,.95,2.2,.1,M.armour,0,1.10,-.1); prof([[1.0,.62],[1.45,.80],[1.45,1.35],[1.0,1.57]],.90,M.armour); prof([[1.30,1.0],[1.47,1.06],[1.47,1.30],[1.25,1.42]],.70,M.lens);
  for(const sd of [-1,1]){ const st=new T.Mesh(new T.PlaneGeometry(.5,.12),stencil('CARGO 16','#d59a4a',256,48)); st.position.set(sd*.552,1.05,-.1); st.rotation.y=sd*Math.PI/2; g.add(st); }
  cbox(K,3.6,.10,.75,.03,M.armour,0,1.62,.2,0,0,0); box(3.6,.012,.012,A,0,1.62,.58).castShadow=false;
  for(const sd of [-1,1]){ const bx=sd*1.45; for(const [z0,z1] of [[-2.0,-1.75],[-.75,.75],[1.75,1.95]]) cbox(K,.30,.30,z1-z0,.06,M.armour,bx,1.55,(z0+z1)/2);
    for(const z of [1.25,-1.25]){ const dg=new T.Group(); dg.position.set(bx,1.36,z); g.add(dg); armDuct(dg,.40,.34,o,sd+z,LIT(on,z>0)); }
    const fin=new T.Shape([[-2.0,0],[-1.55,0],[-1.8,.55],[-2.0,.58]].map(q=>new T.Vector2(q[0],q[1]))); const fgeo=new T.ExtrudeGeometry(fin,{depth:.05,bevelEnabled:false}); fgeo.rotateY(-Math.PI/2); fgeo.translate(.025,0,0); K.add(fgeo,M.armour,bx,1.70,0);
    light(A,bx,2.28,-1.98,.016); light(M.white,bx,1.55,1.96,.016);
    if(!o.gearUp) for(const z of [-.5,.5]) legStrut(K,V(bx,1.40,z),V(bx,.05,z)); }
  cbox(K,2.9,.12,.28,.03,M.armour,0,1.72,-1.88); light(M.white,0,1.10,1.46,.016); return g; }

// 5 DISC LIFTER: a flying saucer of a freighter: one huge central lift fan in an armoured disc, louvred rim vents,
//    the module docked under the hub (real basis: Avro VZ-9 Avrocar)
function o6_5(o){ const g=new T.Group(), K=kit(g), {box,cyl,tube,light,plan,add}=K, on=o.on!==false, A=on?M.amber:M.amberDim;
  const disc=(r0,r1,y,h,mat)=>ring(K,0,y,0,r0,r1,h,mat);
  disc(.95,2.15,1.10,.18,M.armour); disc(1.0,1.90,1.28,.10,M.armour); disc(.97,1.15,1.38,.05,M.shell2); disc(.90,2.18,1.02,.08,M.shell2);
  const rim=new T.Mesh(new T.TorusGeometry(2.17,.012,6,96),A); rim.rotation.x=Math.PI/2; rim.position.y=1.20; g.add(rim);
  for(let i=0;i<24;i++){ const a=i/24*TAU; box(.012,.10,.22,M.black,Math.sin(a)*2.155,1.17,Math.cos(a)*2.155,0,a,0); }
  fanHole(g,0,1.0,0,.92,.40,LIT(on,true),o,0); cyl(.32,.25,12,M.armour,0,1.55,0); cyl(.20,.12,12,M.shell2,0,1.72,0);
  for(let i=0;i<3;i++){ const a=i/3*TAU; box(.06,.06,.95,M.armour,Math.sin(a)*.48,1.40,Math.cos(a)*.48,0,a,0); }
  const vs=new T.Group(); vs.position.set(0,1.22,1.85); g.add(vs); kit(vs).prof([[0,-.12],[.32,-.05],[.36,.08],[0,.16]],.9,M.armour); kit(vs).prof([[.18,.02],[.35,.06],[.25,.12],[.12,.12]],.7,M.lens);
  cargoModule(K,1.0,.65,1.40,0,.55,0); for(const [sx,sz] of [[1,1],[-1,1],[1,-1],[-1,-1]]) tube(V(sx*.62,1.02,sz*.75),V(sx*.52,.82,sz*.62),.03,M.steel,6);
  if(!o.gearUp) for(let i=0;i<4;i++){ const a=Math.PI/4+i*Math.PI/2; legStrut(K,V(Math.sin(a)*1.3,1.02,Math.cos(a)*1.3),V(Math.sin(a)*1.6,.05,Math.cos(a)*1.6)); }
  light(M.white,0,1.25,2.17,.02); light(A,0,1.80,0,.016); return g; }

// 6 TAIL-SITTER: stands on its four fins, X-wing with a prop on each tip, cargo pod in the middle; tips over to fly
//    like a plane (real basis: Bell APT 70)
function o6_6(o){ const g=new T.Group(), K=kit(g), {box,cyl,tube,light,add}=K, on=o.on!==false, A=on?M.amber:M.amberDim;
  const b=new T.Group(); g.add(b); if(o.cruise){ b.rotation.x=Math.PI/2; b.position.set(0,1.6,-1.2); } const BK=kit(b);
  const body=new T.LatheGeometry([[0,.15],[.35,.25],[.50,.9],[.50,2.1],[.38,2.6],[.12,2.9],[0,2.95]].map(q=>new T.Vector2(q[0],q[1])),8,Math.PI/8); body.scale(1.15,1,.85); BK.add(body,M.armour,0,0,0);
  const nose=new T.LatheGeometry([[.385,2.6],[.13,2.9],[0,2.96]].map(q=>new T.Vector2(q[0],q[1])),8,Math.PI/8); nose.scale(1.16,1,.86); BK.add(nose,M.lens,0,0,0);
  BK.box(.012,1.0,.012,A,.58,1.5,0).castShadow=false; BK.box(.012,1.0,.012,A,-.58,1.5,0).castShadow=false;
  for(const sd of [-1,1]){ const st=new T.Mesh(new T.PlaneGeometry(.12,.5),stencil('CARGO 16','#d59a4a',48,256,'700 22px "DejaVu Sans Mono", monospace')); st.position.set(0,1.5,sd*.43); st.rotation.y=sd>0?0:Math.PI; }
  for(let i=0;i<4;i++){ const a=Math.PI/4+i*Math.PI/2, ca=Math.cos(a), sa=Math.sin(a); const w=new T.Group(); w.rotation.y=a; b.add(w); const WK=kit(w);
    WK.box(.08,.70,1.65,M.armour,0,1.55,.90); WK.box(.084,.012,1.6,A,0,1.90,.90).castShadow=false;
    WK.box(.06,.60,.60,M.armour,0,.30,.55); WK.box(.10,.04,.30,M.haz,0,.02,.70);
    const nv=new T.Group(); nv.position.set(0,1.95,1.72); w.add(nv); nacelle(nv,o,LIT(on,i<2),i,i%2?1:-1); }
  light(M.white,0,2.96,0,.016); return g; }

// 7 SHROUDED COAX: a toroidal shroud round a counter-rotating coaxial rotor, a central hub, the module hung below
//    on four struts (real basis: Sikorsky Cypher)
function o6_7(o){ const g=new T.Group(), K=kit(g), {box,cyl,tube,light,add}=K, on=o.on!==false, A=on?M.amber:M.amberDim;
  const tor=new T.Mesh(new T.TorusGeometry(1.75,.30,10,40),M.armour); tor.rotation.x=Math.PI/2; tor.scale.set(1,1,1.4); tor.position.y=2.0; tor.castShadow=true; g.add(tor);
  const lip=new T.Mesh(new T.TorusGeometry(1.48,.012,6,80),A); lip.rotation.x=Math.PI/2; lip.position.y=2.30; g.add(lip);
  for(let i=0;i<4;i++){ const a=i/4*TAU+Math.PI/4; box(1.15,.10,.12,M.armour,Math.sin(a)*.95,2.0,Math.cos(a)*.95,0,a+Math.PI/2,0); }
  cyl(.42,.55,10,M.armour,0,2.0,0); cyl(.30,.18,10,M.shell2,0,2.35,0); cyl(.32,.012,10,A,0,2.20,0);
  for(const [y,d] of [[2.32,1],[1.68,-1]]){ const r=new T.Group(); r.position.y=y; g.add(r); const p=new T.Group(); r.add(p); prop2(p,0,1.40,o.spin,d,(o.phase??0)+(d>0?0:.5),3,3.4); }
  const vs=new T.Group(); vs.position.set(0,1.85,1.9); g.add(vs); kit(vs).box(.5,.22,.20,M.armour,0,0,0); kit(vs).box(.4,.08,.02,M.lens,0,.02,.105); light(M.white,0,1.85,2.02,.016);
  for(const [sx,sz] of [[1,1],[-1,1],[1,-1],[-1,-1]]) tube(V(sx*.30,1.75,sz*.30),V(sx*.55,1.05,sz*.80),.045,M.armour,8);
  cargoModule(K,1.10,.80,1.60,0,.62,0);
  if(!o.gearUp) for(const [sx,sz] of [[1,1],[-1,1],[1,-1],[-1,-1]]) legStrut(K,V(sx*1.3,1.75,sz*1.3),V(sx*1.55,.05,sz*1.55));
  light(A,0,2.46,0,.016); return g; }

// 8 HOVER BARGE: a flying flatbed: six fans in the deck round an open cargo well, a roll cage over the module,
//    a raised bridge pod at the front, rails and tie-downs
function o6_8(o){ const g=new T.Group(), K=kit(g), {box,cyl,tube,light,prof}=K, on=o.on!==false, A=on?M.amber:M.amberDim;
  const OUT=[[1.15,1.75],[1.25,1.55],[1.25,-1.65],[1.1,-1.8],[-1.1,-1.8],[-1.25,-1.65],[-1.25,1.55],[-1.15,1.75]];
  const FANS=[[.82,1.05,.34],[-.82,1.05,.34],[.82,-.05,.34],[-.82,-.05,.34],[.82,-1.15,.34],[-.82,-1.15,.34]];
  slab(K,OUT,FANS.map(f=>[f[0],f[1],f[2]+.01]),.55,.42,M.armour); slab(K,scl(OUT,.97),FANS.map(f=>[f[0],f[1],f[2]+.06]),.97,.03,M.shell2);
  for(const [x,z,r] of FANS) fanHole(g,x,.53,z,r,.44,LIT(on,z>0),o,x+z);
  box(.70,.012,3.0,M.black,0,1.0,-.05);
  cargoModule(K,.95,.75,2.10,0,1.38,-.25);
  for(const z of [-1.35,-.25,.85]){ tube(V(-.55,1.0,z),V(-.55,1.85,z),.03,M.steel,6); tube(V(.55,1.0,z),V(.55,1.85,z),.03,M.steel,6); tube(V(-.55,1.85,z),V(.55,1.85,z),.03,M.steel,6); }
  tube(V(-.55,1.85,-1.35),V(-.55,1.85,.85),.03,M.steel,6); tube(V(.55,1.85,-1.35),V(.55,1.85,.85),.03,M.steel,6);
  const br=new T.Group(); br.position.set(0,1.0,1.25); g.add(br); kit(br).prof([[0,0],[.45,0],[.30,.35],[0,.38]],1.1,M.armour); kit(br).prof([[.33,.10],[.44,.04],[.32,.30],[.26,.32]],.9,M.lens);
  for(const sd of [-1,1]){ box(.006,.016,3.2,A,sd*1.253,.85,-.05).castShadow=false; for(let k=0;k<5;k++) box(.06,.06,.06,M.anod,sd*1.1,1.02,-1.4+k*.65); }
  if(!o.gearUp) for(const z of [-1.4,1.3]) for(const sx of [-1,1]) legStrut(K,V(sx*.4,.56,z),V(sx*.45,.05,z));
  light(M.white,0,.75,1.76,.02); light(A,0,.75,-1.81,.02); return g; }

// 9 TANDEM-WING: a deep cargo fuselage with a low front wing and a high rear wing, two fans buried in each wing,
//    wingtip fins joining nothing; side cargo door
function o6_9(o){ const g=new T.Group(), K=kit(g), {box,cyl,tube,light,plan}=K, on=o.on!==false, A=on?M.amber:M.amberDim;
  fus(K,[[-1.9,.15],[-1.5,.42],[-.9,.55],[1.0,.55],[1.5,.42],[1.85,.12]],1.0,1.0,1.10,M.armour); fus(K,[[1.25,.50],[1.6,.33],[1.83,.15]],1.0,1.0,1.10,M.lens);
  for(const sd of [-1,1]){ box(.006,.55,.95,M.shell2,sd*.51,1.05,-.2); const st=new T.Mesh(new T.PlaneGeometry(.45,.11),stencil('CARGO 16','#d59a4a',256,48)); st.position.set(sd*.518,1.05,-.2); st.rotation.y=sd*Math.PI/2; g.add(st); }
  const W1=[[.4,1.25],[2.05,.95],[2.05,.35],[.4,.40]], W2=[[.4,-.60],[2.05,-.80],[2.05,-1.45],[.4,-1.50]];
  for(const sd of [-1,1]){ const w1=W1.map(q=>[q[0]*sd,q[1]]), w2=W2.map(q=>[q[0]*sd,q[1]]);
    slab(K,w1,[[sd*1.30,.72,.28]],.80,.12,M.armour); fanHole(g,sd*1.30,.78,.72,.28,.15,LIT(on,true),o,sd);
    slab(K,w2,[[sd*1.30,-1.12,.28]],1.62,.12,M.armour); fanHole(g,sd*1.30,1.60,-1.12,.28,.15,LIT(on,false),o,sd+2);
    box(.06,.95,.60,M.armour,sd*2.08,1.20,.65); box(.06,.70,.60,M.armour,sd*2.08,1.40,-1.12); light(A,sd*2.08,1.70,-1.12,.016); light(M.white,sd*2.08,1.70,.65,.016);
    if(!o.gearUp) for(const z of [-.9,.6]) legStrut(K,V(sd*.40,.70,z),V(sd*.60,.05,z)); }
  cbox(K,.20,.55,.80,.04,M.armour,0,1.80,-1.10); box(.012,.012,1.6,A,0,1.66,-.2).castShadow=false; light(M.white,0,1.10,1.86,.016); return g; }

// 10 OCTO-DUCT HEAVY: a square armoured body, eight ducts on four forked arms, forklift tines under the module
//    (real basis: Griff 300 heavy multirotor)
function o6_10(o){ const g=new T.Group(), K=kit(g), {box,cyl,tube,light,plan}=K, on=o.on!==false, A=on?M.amber:M.amberDim;
  plan([[.65,.8],[.8,.65],[.8,-.65],[.65,-.8],[-.65,-.8],[-.8,-.65],[-.8,.65],[-.65,.8]],.50,M.armour,1.55); plan([[.5,.6],[.6,.5],[.6,-.5],[.5,-.6],[-.5,-.6],[-.6,-.5],[-.6,.5],[-.5,.6]],.12,M.shell2,2.05);
  box(1.62,.012,.012,A,0,1.80,.805).castShadow=false;
  const vs=new T.Group(); vs.position.set(0,1.70,.80); g.add(vs); kit(vs).box(.9,.20,.10,M.armour,0,0,.05); kit(vs).box(.8,.07,.02,M.lens,0,.02,.105); light(M.white,0,1.72,.92,.016);
  for(let i=0;i<4;i++){ const a=Math.PI/4+i*Math.PI/2; const ag=new T.Group(); ag.rotation.y=a; ag.position.y=1.80; g.add(ag); const AK=kit(ag);
    cbox(AK,.22,.20,1.05,.04,M.armour,0,0,1.15); AK.box(.012,.012,1.0,A,0,.105,1.15).castShadow=false;
    for(const s2 of [-1,1]){ AK.tube(V(0,0,1.55),V(s2*.42,0,1.80),.06,M.armour,6); const dg=new T.Group(); dg.position.set(s2*.48,-.18,1.85); ag.add(dg); armDuct(dg,.32,.30,o,i*2+s2,LIT(on,Math.cos(a)>0)); } }
  for(const sx of [-1,1]){ box(.12,.85,.12,M.armour,sx*.40,1.12,.70); box(.12,.06,1.70,M.haz,sx*.40,.68,.0); }
  cargoModule(K,1.05,.70,1.60,0,1.06,0);
  if(!o.gearUp) for(const [sx,sz] of [[1,1],[-1,1],[1,-1],[-1,-1]]) legStrut(K,V(sx*.7,1.56,sz*.7),V(sx*.95,.05,sz*.95));
  light(A,0,2.18,0,.016); return g; }

// 11 LIFTING-BODY SHUTTLE: a smooth wide lifting body; four lift fans under split doors that open on top, two pusher
//    ducts at the tail, twin fins; the cargo bay inside behind a ventral door
function o6_11(o){ const g=new T.Group(), K=kit(g), {box,cyl,tube,light,plan}=K, on=o.on!==false, A=on?M.amber:M.amberDim;
  const OUT=[[0,1.95],[.6,1.6],[1.25,.6],[1.35,-.9],[1.1,-1.5],[-1.1,-1.5],[-1.35,-.9],[-1.25,.6],[-.6,1.6]];
  const FANS=[[.62,.45,.30],[-.62,.45,.30],[.70,-.75,.30],[-.70,-.75,.30]];
  slab(K,OUT,FANS.map(f=>[f[0],f[1],f[2]+.02]),.70,.28,M.armour); slab(K,scl(OUT,.92),FANS.map(f=>[f[0],f[1],f[2]+.05]),.98,.10,M.armour); slab(K,scl(OUT,.7),[],1.08,.10,M.armour);
  slab(K,[[0,1.55],[.3,1.25],[.35,.85],[-.35,.85],[-.3,1.25]],[],1.08,.11,M.lens);
  for(const [x,z,r] of FANS){ fanHole(g,x,.68,z,r,.40,LIT(on,z>0),o,x+z); for(const sd of [-1,1]){ const dp=new T.Group(); dp.position.set(x+sd*(r+.02),1.08,z); dp.rotation.z=sd*1.15; g.add(dp); kit(dp).box(r,.03,2*r,M.armour,-sd*r/2,0,0); } }
  for(const sd of [-1,1]){ const pg=new T.Group(); pg.position.set(sd*.55,.95,-1.55); pg.rotation.x=Math.PI/2; g.add(pg); ductRing(pg,.24,.22,M.duct,A); fanCore(pg,.24,.22,o,sd);
    const fin=new T.Shape([[-1.5,0],[-.95,0],[-1.3,.55],[-1.5,.58]].map(q=>new T.Vector2(q[0],q[1]))); const fgeo=new T.ExtrudeGeometry(fin,{depth:.05,bevelEnabled:false}); fgeo.rotateY(-Math.PI/2); fgeo.translate(.025,0,0); const fm=new T.Mesh(fgeo,M.armour); fm.position.set(sd*.95,1.05,0); fm.rotation.z=-sd*.35; fm.castShadow=true; g.add(fm); }
  edgeStrip(K,[.62,1.58],[1.23,.62],-1,.85,A); edgeStrip(K,[-.62,1.58],[-1.23,.62],1,.85,A); light(M.white,0,.85,1.96,.02);
  box(.9,.006,1.4,M.black,0,.698,-.3); if(!o.gearUp) for(const [x,z] of [[0,1.2],[.6,-.9],[-.6,-.9]]){ tube(V(x,.70,z),V(x,.10,z),.03,M.steel,8); cyl(.10,.08,14,M.rubber,x,.10,z,0,0,Math.PI/2); }
  return g; }

// 12 WEDGE: a faceted stealth wedge, chined edges, four fans under armoured grilles, canted fins, belly bay doors
function o6_12(o){ const g=new T.Group(), K=kit(g), {box,cyl,tube,light,plan,add}=K, on=o.on!==false, A=on?M.amber:M.amberDim;
  const OUT=[[0,2.0],[1.35,-.4],[1.55,-1.2],[.9,-.95],[0,-1.4],[-.9,-.95],[-1.55,-1.2],[-1.35,-.4]];
  const FANS=[[.72,-.05,.28],[-.72,-.05,.28],[1.05,-.70,.24],[-1.05,-.70,.24]];
  slab(K,scl(OUT,.97),FANS.map(f=>[f[0],f[1],f[2]+.03]),.72,.04,M.shell2);
  slab(K,OUT,FANS.map(f=>[f[0],f[1],f[2]+.01]),.76,.15,M.armour);
  const SP=[[0,1.95],[.36,.45],[.34,-1.15],[0,-1.38],[-.34,-1.15],[-.36,.45]];
  plan(SP,.16,M.armour,.91); plan(scl(SP,.62),.08,M.shell2,1.07); plan([[0,1.82],[.20,1.05],[-.20,1.05]],.012,M.lens,1.07);
  box(.012,.006,1.6,A,0,1.152,-.25).castShadow=false;
  for(const [x,z,r] of FANS){ fanHole(g,x,.72,z,r,.19,LIT(on,z>-.4),o,x+z); for(let k=-2;k<=2;k++){ const L=2*Math.sqrt(Math.max(0,r*r-(k*.095)**2))-.02; box(.018,.03,L,M.shell2,x+k*.095,.895,z); } }
  for(const sd of [-1,1]){ const fin=new T.Shape([[-1.15,0],[-.70,0],[-.98,.45],[-1.18,.48]].map(q=>new T.Vector2(q[0],q[1]))); const fgeo=new T.ExtrudeGeometry(fin,{depth:.04,bevelEnabled:false}); fgeo.rotateY(-Math.PI/2); fgeo.translate(.02,0,0); const fm=new T.Mesh(fgeo,M.armour); fm.position.set(sd*.30,1.05,0); fm.rotation.z=-sd*.6; fm.castShadow=true; g.add(fm);
    edgeStrip(K,[sd*.05,1.95],[sd*1.33,-.38],sd,.83,A); light(A,sd*1.55,.83,-1.2,.016); }
  box(.9,.006,1.3,M.black,0,.718,-.3); light(M.white,0,.83,2.0,.016);
  if(!o.gearUp) for(const [x,z] of [[0,1.2],[.55,-.95],[-.55,-.95]]){ tube(V(x,.74,z),V(x,.10,z),.03,M.steel,8); cyl(.10,.08,14,M.rubber,x,.10,z,0,0,Math.PI/2); }
  return g; }

// ===================================================================== T6 DISC FREIGHTER (Justin picked option 5)
// An armoured saucer round one big central lift fan; the flow turns out under the disc and leaves through louvred rim
// vents past a pivoting focusing ring (that is how it steers); a hub nacelle on three struts over the fan with a sensor
// crown; a sensor blister at the front; the 16-block module docks under the hub on a clamp ring; four retracting legs.
function t6(o){
  const g=new T.Group(), K=kit(g), {box,cyl,tube,light,add}=K, on=o.on!==false, A=on?M.amber:M.amberDim;
  const P=[[1.0,1.02],[1.95,1.02],[2.12,1.08],[2.18,1.16],[2.15,1.24],[1.95,1.32],[1.6,1.38],[1.15,1.42],[1.0,1.42],[1.0,1.02]];
  const dg=new T.LatheGeometry(P.map(q=>new T.Vector2(q[0],q[1])),48); K.add(dg,M.armour,0,0,0);
  ring(K,0,.98,0,1.0,1.90,.04,M.shell2); ring(K,0,1.42,0,1.0,1.14,.03,M.shell2);
  const yAt=r=>r<1.15?1.42:r<1.6?1.42-(r-1.15)/.45*.04:r<1.95?1.38-(r-1.6)/.35*.06:1.32;
  for(const r of [1.45,1.80]){ const t2=new T.Mesh(new T.TorusGeometry(r,.006,4,96),M.black); t2.rotation.x=Math.PI/2; t2.position.y=yAt(r)+.004; g.add(t2); }
  for(let i=0;i<12;i++){ const a=i/12*TAU+TAU/24; const r0=1.64,r1=1.86; const m=box(.008,.008,r1-r0,M.black,Math.sin(a)*(r0+r1)/2,(yAt(r0)+yAt(r1))/2+.004,Math.cos(a)*(r0+r1)/2); m.rotation.set(0,a,0,'YXZ'); m.rotation.x=Math.atan2(yAt(r0)-yAt(r1),r1-r0)*-1; }
  // rim band (white arc at the front, amber elsewhere), louvred vents under the rim, focusing ring on struts
  { const t2=new T.Mesh(new T.TorusGeometry(2.183,.014,6,96,TAU*5/6),A); t2.rotation.set(Math.PI/2,0,Math.PI/2+TAU/12); t2.position.y=1.16; g.add(t2);
    const t3=new T.Mesh(new T.TorusGeometry(2.183,.014,6,24,TAU/6),on?M.white:M.amberDim); t3.rotation.set(Math.PI/2,0,Math.PI/2-TAU/12); t3.position.y=1.16; g.add(t3); }
  for(let i=0;i<40;i++){ const a=i/40*TAU; const m=box(.016,.05,.17,M.black,Math.sin(a)*2.04,1.045,Math.cos(a)*2.04); m.rotation.y=a; }
  { const fr=new T.Group(); fr.rotation.x=(o.cruise||0)*.08; fr.position.y=.93; g.add(fr); const t2=new T.Mesh(new T.TorusGeometry(2.02,.035,6,80),M.armour); t2.rotation.x=Math.PI/2; t2.castShadow=true; fr.add(t2);
    for(let i=0;i<8;i++){ const a=i/8*TAU+TAU/16; kit(fr).box(.04,.10,.04,M.anod,Math.sin(a)*2.02,.06,Math.cos(a)*2.02); } }
  // central fan, hub nacelle on three struts, sensor crown, strobe
  fanHole(g,0,1.0,0,.97,.42,on?M.white:M.amberDim,o,0);
  cyl(.34,.30,12,M.armour,0,1.58,0); cyl(.36,.02,12,A,0,1.66,0); cyl(.24,.12,12,M.shell2,0,1.79,0); add(new T.SphereGeometry(.14,12,8,0,TAU,0,Math.PI/2),M.lens,0,1.85,0);
  light(M.white,0,2.0,0,.016); tube(V(0,1.85,0),V(0,1.99,0),.01,M.anod,6);
  for(let i=0;i<3;i++){ const a=i/3*TAU+Math.PI/3; const s0=V(Math.sin(a)*.32,1.55,Math.cos(a)*.32), s1=V(Math.sin(a)*1.05,1.43,Math.cos(a)*1.05); tube(s0,s1,.05,M.armour,6); tube(s0.clone().add(V(0,.08,0)),s1.clone().add(V(0,.04,0)),.012,M.steel,4); }
  // front sensor blister with a visor, side nav lights, battery hatches and gauges, aerials, stencil
  { const sb=new T.Group(); sb.position.set(0,1.34,1.68); g.add(sb); const SK=kit(sb); SK.prof([[-.24,0],[.22,0],[.10,.14],[-.18,.16]],.62,M.armour); SK.prof([[.0,.08],[.21,.02],[.10,.13],[-.02,.145]],.50,M.lens); SK.light(M.white,0,.06,.23,.018,sb,.26); }
  for(const sd of [-1,1]) light(A,sd*2.19,1.16,0,.02,g,.3);
  for(const sd of [-1,1]){ const hg=new T.Group(); const a=sd*2.3+Math.PI; hg.position.set(Math.sin(Math.PI+sd*.7)*1.62,yAt(1.62)+.006,Math.cos(Math.PI+sd*.7)*1.62); hg.rotation.y=Math.PI+sd*.7; g.add(hg); const HK=kit(hg);
    HK.box(.34,.012,.22,M.batt,0,0,0); for(let k=0;k<5;k++) HK.box(.04,.008,.03,k<(o.charge??5)?M.white:M.black,-.10+k*.05,.008,.06); }
  for(const sd of [-1,1]) box(.012,.12,.10,M.shell2,sd*.6,1.44,-1.3,0,0,0);
  { const st=new T.Mesh(new T.PlaneGeometry(.30,.08),stencil('DL-T6')); st.rotation.x=-Math.PI/2; st.position.set(0,yAt(1.45)+.012,-1.45); st.rotation.z=Math.PI; g.add(st); }
  // clamp ring under the hub, four clamp arms, the module (lowered on cables when o.cargo is 'winch')
  ring(K,0,.88,0,.55,.78,.10,M.anod); for(const [sx,sz] of [[1,1],[-1,1],[1,-1],[-1,-1]]){ tube(V(sx*.40,.94,sz*.40),V(sx*.50,.80,sz*.62),.03,M.steel,6); box(.06,.16,.10,M.haz,sx*.51,.78,sz*.64); }
  if(o.cargo){ const d=o.cargo==='winch'?(o.drop??0):0; const cg=new T.Group(); cg.position.y=-d; g.add(cg); cargoModule(kit(cg),1.0,.62,1.40,0,.55,0);
    if(d>.02) for(const [sx,sz] of [[1,1],[-1,1],[1,-1],[-1,-1]]) tube(V(sx*.45,.88,sz*.45),V(sx*.45,.86-d,sz*.45),.006,M.tether,4); }
  light(A,0,.83,0,.016,g,.22);
  // four legs that fold up in flight
  if(!o.gearUp) for(let i=0;i<4;i++){ const a=Math.PI/4+i*Math.PI/2; legStrut(K,V(Math.sin(a)*1.30,1.0,Math.cos(a)*1.30),V(Math.sin(a)*1.62,.05,Math.cos(a)*1.62),.30); }
  return g; }

// ===================================================================== T7 OPTIONS: TWELVE LARGE HYBRID AEROSTATS (hydrogen lift cells + ducted fans, 32 blocks)
const envTex=ctex(256,128,(x,w,h)=>{ x.fillStyle='#2d3237'; x.fillRect(0,0,w,h); for(let i=0;i<w;i+=16){ x.fillStyle='#24282c'; x.fillRect(i,0,2,h); x.fillStyle='#3a4046'; x.fillRect(i+2,0,1,h); } x.fillStyle='rgba(0,0,0,.25)'; x.fillRect(0,h/2,w,2); });
envTex.wrapS=envTex.wrapT=T.RepeatWrapping; envTex.repeat.set(2,1);
M.env=std({map:envTex, roughness:.85, metalness:.05}); M.envD=std({map:envTex, roughness:.85, metalness:.05, side:T.DoubleSide});
// ogive envelope along z, length L, radius r
function env(K,L,r,y,sx=1,sy=1,mat=M.env,z=0,segs=28){ const pts=[[-L/2,.02],[-L/2+.06*L,.45*r],[-L/2+.18*L,.82*r],[-L/6,.99*r],[0,r],[L/5,.98*r],[L/2-.16*L,.80*r],[L/2-.05*L,.45*r],[L/2,.02]].map(q=>[q[0]+z,q[1]]);
  return fus(K,pts,sx,sy,y,mat,segs,0); }
function bandRing(K,r,y,z,sx,sy,mat){ const t2=new T.Mesh(new T.TorusGeometry(r,.014,6,48),mat); t2.scale.set(sx,sy,1); t2.position.set(0,y,z); K.add(new T.BoxGeometry(.001,.001,.001),M.black,0,0,0); return t2; }
function fin(K,pts,x,y,z,rz,th=.05,mat=M.armour){ const f=new T.Shape(pts.map(q=>new T.Vector2(q[0],q[1]))); const ge=new T.ExtrudeGeometry(f,{depth:th,bevelEnabled:false}); ge.rotateY(-Math.PI/2); ge.translate(th/2,0,0); const m=K.add(ge,mat,x,y,z); m.rotation.z=rz; return m; }
function gondola(K,w,h,l,y,z,o,A){ cbox(K,w,h,l,.08,M.armour,0,y,z); K.box(w*.8,.06,.02,M.lens,0,y+.05,z+l/2+.005); K.box(w+.004,.012,l*.9,A,0,y+h/2-.06,z).castShadow=false; }
function podDuct(g,x,y,z,R,o,lit,rx=0){ const dg=new T.Group(); dg.position.set(x,y,z); dg.rotation.x=rx; g.add(dg); ductRing(dg,R,R*.8,M.duct,lit); fanCore(dg,R,R*.8,o,x+z); return dg; }
const L7=(on,f)=>on?(f?M.white:M.amber):M.amberDim;
function rig(K,a,b){ K.tube(a,b,.008,M.steel,4); }

// 1 TRILOBE HYBRID: three fused lobes for aerodynamic lift as well as buoyancy, four vectoring ducts, air-cushion pads (real: Lockheed P-791)
function o7_1(o){ const g=new T.Group(), K=kit(g), on=o.on!==false, A=on?M.amber:M.amberDim;
  for(const x of [-.95,0,.95]) env(K,7.0-(x?.4:0),1.05,2.55+(x?0:.15),1,.72,M.env,0).position.x=x;
  fin(K,[[-3.3,0],[-2.4,0],[-2.9,.9],[-3.4,.95]],0,3.15,0,0,.06); for(const sd of [-1,1]) fin(K,[[-3.2,0],[-2.3,0],[-2.9,.7],[-3.3,.72]],sd*1.0,3.0,0,-sd*.9,.06);
  gondola(K,1.1,.55,2.6,1.55,.6,o,A); for(const sd of [-1,1]){ K.tube(V(sd*.4,1.82,1.5),V(sd*.6,2.0,1.6),.03,M.steel,6); K.tube(V(sd*.4,1.82,-.4),V(sd*.6,2.0,-.6),.03,M.steel,6); }
  for(const [x,z] of [[2.2,1.5],[-2.2,1.5],[2.2,-1.8],[-2.2,-1.8]]){ K.tube(V(Math.sign(x)*1.5,2.3,z),V(x,2.2,z),.05,M.armour,6); podDuct(g,x,1.95,z,.45,o,L7(on,z>0)); }
  for(const z of [-2.2,-.8,.8,2.2]) for(const sd of [-1,1]){ K.box(.6,.12,.6,M.shell2,sd*1.0,1.86,z); K.box(.62,.012,.62,M.haz,sd*1.0,1.80,z); }
  K.light(M.white,0,2.55,3.52,.03,g,.4); K.light(A,0,4.1,-3.2,.025,g,.35);
  if(o.cargo){ for(const x of [-.3,.3]) crateBox(K,.5,.5,.6,x,1.03,.6); }
  return g; }

// 2 OPEN-FRAME RIGID: a lattice hull with the gas cells visible through it, four swivelling ducts, keel walkway and cargo cage
function o7_2(o){ const g=new T.Group(), K=kit(g), on=o.on!==false, A=on?M.amber:M.amberDim;
  env(K,6.6,1.12,2.6,1,1,M.env,0,24);
  for(let i=0;i<12;i++){ const a=i/12*TAU; const pts=[[-3.3,.05],[-2.6,.8],[-1.2,1.18],[0,1.25],[1.2,1.18],[2.6,.8],[3.4,.05]]; for(let k=0;k<pts.length-1;k++){ const [z0,r0]=pts[k],[z1,r1]=pts[k+1]; K.tube(V(Math.sin(a)*r0,2.6+Math.cos(a)*r0,z0),V(Math.sin(a)*r1,2.6+Math.cos(a)*r1,z1),.022,M.armour,4); } }
  for(const [z,r] of [[-2.6,.8],[-1.2,1.18],[0,1.25],[1.2,1.18],[2.6,.8]]){ const t2=new T.Mesh(new T.TorusGeometry(r,.03,4,12),M.armour); t2.position.set(0,2.6,z); t2.rotation.z=Math.PI/12; g.add(t2); }
  K.box(.5,.14,5.4,M.shell2,0,1.32,0); K.box(.52,.012,5.2,A,0,1.25,0).castShadow=false;
  for(const sd of [-1,1]) fin(K,[[-3.5,0],[-2.6,0],[-3.0,.9],[-3.5,.95]],0,3.6,0,0); fin(K,[[-3.5,0],[-2.6,0],[-3.0,.9],[-3.5,.95]],0,1.65,0,Math.PI);
  for(const [x,z] of [[1.55,1.6],[-1.55,1.6],[1.55,-1.6],[-1.55,-1.6]]){ K.tube(V(Math.sign(x)*.9,2.0,z),V(x,2.0,z),.04,M.armour,6); podDuct(g,x+Math.sign(x)*.35,1.75,z,.40,o,L7(on,z>0)); }
  const cg=new T.Group(); g.add(cg); const CK=kit(cg); for(const x of [-.45,.45]) for(const z of [-.55,.55]) CK.tube(V(x,1.25,z),V(x,.45,z),.025,M.steel,6); CK.box(1.1,.05,1.3,M.anod,0,.45,0); if(o.cargo) for(const x of [-.25,.25]) for(const z of [-.3,.3]) crateBox(CK,.45,.45,.5,x,.70,z);
  if(!o.gearUp) for(const z of [-2.0,2.0]) legStrut(K,V(0,1.25,z),V(0,.05,z),.4);
  K.light(M.white,0,2.6,3.4,.03,g,.4); return g; }

// 3 LENTICULAR: a lens-shaped disc envelope, six ducts round the rim, a central cargo core hanging below
function o7_3(o){ const g=new T.Group(), K=kit(g), on=o.on!==false, A=on?M.amber:M.amberDim;
  const pts=[[0,.02],[.8,2.95],[1.25,2.95]].map(q=>q); const lg=new T.LatheGeometry([[0,1.9],[1.2,1.98],[2.4,2.2],[2.95,2.6],[2.4,3.0],[1.2,3.25],[0,3.32]].map(q=>new T.Vector2(q[0],q[1])),40); K.add(lg,M.env,0,0,0);
  const r2=new T.Mesh(new T.TorusGeometry(2.96,.05,6,64),M.armour); r2.rotation.x=Math.PI/2; r2.position.y=2.6; g.add(r2); const r3=new T.Mesh(new T.TorusGeometry(2.99,.012,6,64),A); r3.rotation.x=Math.PI/2; r3.position.y=2.6; g.add(r3);
  for(let i=0;i<6;i++){ const a=i/6*TAU+TAU/12; const x=Math.sin(a)*3.25, z=Math.cos(a)*3.25; K.tube(V(Math.sin(a)*2.95,2.6,Math.cos(a)*2.95),V(x,2.45,z),.05,M.armour,6); podDuct(g,x,2.25,z,.32,o,L7(on,z>1)); }
  K.cyl(.7,.9,10,M.armour,0,1.45,0); K.cyl(.72,.012,10,A,0,1.7,0); K.box(.6,.08,.02,M.lens,0,1.6,.69);
  if(o.cargo) for(const x of [-.25,.25]) for(const z of [-.25,.25]) crateBox(K,.45,.45,.45,x,.75,z);
  if(!o.gearUp) for(let i=0;i<3;i++){ const a=i/3*TAU; legStrut(K,V(Math.sin(a)*.6,1.1,Math.cos(a)*.6),V(Math.sin(a)*1.1,.05,Math.cos(a)*1.1),.35); }
  K.light(M.white,0,3.33,0,.03,g,.4); return g; }

// 4 CATAMARAN: two slim envelopes with a cargo deck slung between them, ducts on outriggers, twin tails
function o7_4(o){ const g=new T.Group(), K=kit(g), on=o.on!==false, A=on?M.amber:M.amberDim;
  for(const sd of [-1,1]){ env(K,6.8,.95,2.6,1,1,M.env,0); g.children[g.children.length-1].position.x=sd*1.25;
    fin(K,[[-3.2,0],[-2.4,0],[-2.9,.9],[-3.3,.95]],sd*1.25,3.45,0,0); fin(K,[[-3.2,0],[-2.4,0],[-2.9,.8],[-3.3,.85]],sd*1.25,1.75,0,Math.PI);
    K.light(sd>0?M.white:M.white,sd*1.25,2.6,3.42,.025,g,.35); }
  K.box(1.8,.18,3.6,M.armour,0,1.75,0); for(const z of [-1.4,0,1.4]) K.box(1.6,.10,.18,M.shell2,0,2.2,z); K.box(1.82,.012,3.4,A,0,1.66,0).castShadow=false;
  for(const sd of [-1,1]) for(const z of [-1.4,0,1.4]) K.tube(V(sd*.85,2.25,z),V(sd*1.0,2.0,z),.04,M.armour,6);
  for(const [x,z] of [[2.75,1.4],[-2.75,1.4],[2.75,-1.6],[-2.75,-1.6]]){ K.tube(V(Math.sign(x)*2.1,2.6,z),V(x,2.4,z),.05,M.armour,6); podDuct(g,x,2.15,z,.40,o,L7(on,z>0)); }
  if(o.cargo) for(const x of [-.5,0,.5]) for(const z of [-1.0,-.35,.3,.95]) crateBox(K,.42,.42,.5,x,1.45,z);
  if(!o.gearUp) for(const [x,z] of [[.8,1.6],[-.8,1.6],[.8,-1.6],[-.8,-1.6]]) legStrut(K,V(x,1.66,z),V(x*1.1,.05,z),.35);
  gondola(K,.8,.45,1.0,1.4,1.95,o,A); return g; }

// 5 CAGED SPHERE: a spherical lift cell inside an armoured cage ring with eight fans, cargo hold below
function o7_5(o){ const g=new T.Group(), K=kit(g), on=o.on!==false, A=on?M.amber:M.amberDim;
  const sp=K.add(new T.SphereGeometry(1.75,32,20),M.env,0,2.85,0);
  for(const ry of [0,Math.PI/3,2*Math.PI/3]){ const t2=new T.Mesh(new T.TorusGeometry(1.82,.04,6,48),M.armour); t2.rotation.y=ry; t2.position.y=2.85; t2.castShadow=true; g.add(t2); }
  const eq=new T.Mesh(new T.TorusGeometry(1.86,.07,6,64),M.armour); eq.rotation.x=Math.PI/2; eq.position.y=2.85; g.add(eq); const eb=new T.Mesh(new T.TorusGeometry(1.93,.012,6,64),A); eb.rotation.x=Math.PI/2; eb.position.y=2.85; g.add(eb);
  for(let i=0;i<8;i++){ const a=i/8*TAU; const x=Math.sin(a)*2.35, z=Math.cos(a)*2.35; K.tube(V(Math.sin(a)*1.9,2.85,Math.cos(a)*1.9),V(x,2.85,z),.04,M.armour,6); podDuct(g,x,2.7,z,.30,o,L7(on,Math.cos(a)>.5)); }
  cbox(K,1.4,.85,1.6,.1,M.armour,0,.95,0); K.box(1.0,.07,.02,M.lens,0,1.15,.805); K.tube(V(0,1.38,0),V(0,1.12,0),.12,M.shell2,8);
  if(!o.gearUp) for(const [x,z] of [[.6,.7],[-.6,.7],[.6,-.7],[-.6,-.7]]) legStrut(K,V(x,.55,z),V(x*1.3,.05,z*1.3),.3);
  K.light(M.white,0,4.62,0,.03,g,.4); return g; }

// 6 CELL BUNDLE: six vertical lift-cell cylinders round a cargo core, a ducted fan crowning each cell
function o7_6(o){ const g=new T.Group(), K=kit(g), on=o.on!==false, A=on?M.amber:M.amberDim;
  for(let i=0;i<6;i++){ const a=i/6*TAU+TAU/12, x=Math.sin(a)*1.55, z=Math.cos(a)*1.55;
    const lg=new T.LatheGeometry([[0,.9],[.55,1.0],[.72,1.4],[.72,3.2],[.55,3.6],[0,3.7]].map(q=>new T.Vector2(q[0],q[1])),16); K.add(lg,M.env,x,0,z);
    for(const y of [1.6,2.4,3.0]){ const t2=new T.Mesh(new T.TorusGeometry(.73,.025,4,24),M.armour); t2.rotation.x=Math.PI/2; t2.position.set(x,y,z); g.add(t2); }
    podDuct(g,x,3.72,z,.40,o,L7(on,z>0)); }
  cbox(K,1.4,2.2,1.4,.15,M.armour,0,2.1,0,0,0,0); K.box(1.0,.07,.02,M.lens,0,2.9,.705); for(let k=0;k<5;k++) K.box(1.42,.012,.012,k===2?A:M.black,0,1.5+k*.3,.705);
  for(let i=0;i<6;i++){ const a=i/6*TAU+TAU/12; K.tube(V(Math.sin(a)*.7,2.4,Math.cos(a)*.7),V(Math.sin(a)*.85,2.4,Math.cos(a)*.85),.05,M.steel,6); }
  if(o.cargo){ K.box(1.0,.06,1.0,M.anod,0,.95,0); for(const x of [-.25,.25]) for(const z of [-.25,.25]) crateBox(K,.45,.45,.45,x,.73,z); }
  if(!o.gearUp) for(let i=0;i<3;i++){ const a=i/3*TAU; legStrut(K,V(Math.sin(a)*.6,1.0,Math.cos(a)*.6),V(Math.sin(a)*1.0,.05,Math.cos(a)*1.0),.3); }
  return g; }

// 7 AEROSTATIC WING: a thick gas-filled flying wing, fans along the trailing edge, cargo bay in the centre section
function o7_7(o){ const g=new T.Group(), K=kit(g), on=o.on!==false, A=on?M.amber:M.amberDim;
  const AF=[[1.7,0],[1.45,.32],[.9,.55],[0,.62],[-.9,.48],[-1.6,.15],[-1.75,0],[-1.6,-.12],[-.9,-.30],[0,-.36],[.9,-.30],[1.45,-.16]];
  K.prof(AF,5.0,M.env); for(const sd of [-1,1]){ const cap=K.add(new T.SphereGeometry(1,20,12,0,Math.PI),M.env,sd*2.5,0,0); cap.rotation.y=sd>0?0:Math.PI; cap.scale.set(.45,.5,1.7); cap.position.y=2.3+.12; }
  g.children.filter(c=>c.geometry&&c.geometry.type==='ExtrudeGeometry'&&c.material===M.env).forEach(c=>c.position.y=2.3);
  K.box(5.0,.012,.012,A,0,2.92,0).castShadow=false;
  for(let i=-3;i<=3;i++){ if(i===0) continue; const x=i*.75; const pg=new T.Group(); pg.position.set(x,2.30,-1.95); pg.rotation.x=Math.PI/2; g.add(pg); ductRing(pg,.24,.22,M.duct,A); fanCore(pg,.24,.22,o,i); }
  for(const sd of [-1,1]) fin(K,[[-1.4,0],[-.6,0],[-1.0,.8],[-1.4,.82]],sd*1.3,2.6,0,-sd*.25);
  cbox(K,1.2,.7,2.4,.12,M.armour,0,1.45,.1); K.box(.9,.06,.02,M.lens,0,1.6,1.305); if(o.cargo) for(const x of [-.25,.25]) crateBox(K,.45,.45,.5,x,.88,.1);
  if(!o.gearUp) for(const [x,z] of [[.5,.9],[-.5,.9],[.5,-.7],[-.5,-.7]]) legStrut(K,V(x,1.1,z),V(x*1.2,.05,z),.3);
  for(const sd of [-1,1]) K.light(A,sd*2.95,2.3,0,.025,g,.35); K.light(M.white,0,2.35,1.75,.025,g,.35); return g; }

// 8 MANTA: a broad flat blended envelope like a ray, two big ducts buried in the wings, cargo pod under the body
function o7_8(o){ const g=new T.Group(), K=kit(g), on=o.on!==false, A=on?M.amber:M.amberDim;
  const e=env(K,6.4,1.4,2.3,2.0,.48,M.env,0,32);
  for(const sd of [-1,1]){ const dg=new T.Group(); dg.position.set(sd*1.7,1.85,-.3); g.add(dg); armDuct(dg,.55,.70,o,sd,L7(on,true)); }
  fin(K,[[-3.2,0],[-2.3,0],[-2.8,.8],[-3.25,.85]],0,2.85,0,0,.06);
  const band=new T.Mesh(new T.TorusGeometry(1.4,.012,6,48),A); band.scale.set(2.02,.49,1); band.position.set(0,2.3,0); g.add(band);
  cbox(K,1.3,.75,3.0,.12,M.armour,0,1.25,.4); K.box(1.0,.07,.02,M.lens,0,1.4,1.905); if(o.cargo) for(const z of [-.3,.4,1.1]) crateBox(K,.9,.45,.5,0,.62,z);
  if(!o.gearUp) for(const [x,z] of [[.55,1.4],[-.55,1.4],[.55,-.6],[-.55,-.6]]) legStrut(K,V(x,.88,z),V(x*1.2,.05,z),.3);
  K.light(M.white,0,2.3,3.2,.03,g,.4); for(const sd of [-1,1]) K.light(A,sd*2.8,2.3,0,.025,g,.35); return g; }

// 9 TORUS: a doughnut envelope with the cargo core hanging in its hole, four ducts on the outer rim
function o7_9(o){ const g=new T.Group(), K=kit(g), on=o.on!==false, A=on?M.amber:M.amberDim;
  const tr=new T.Mesh(new T.TorusGeometry(2.05,.85,20,48),M.env); tr.rotation.x=Math.PI/2; tr.scale.set(1,1,.85); tr.position.y=2.6; tr.castShadow=true; g.add(tr);
  const t2=new T.Mesh(new T.TorusGeometry(2.92,.012,6,64),A); t2.rotation.x=Math.PI/2; t2.position.y=2.6; g.add(t2);
  for(let i=0;i<4;i++){ const a=i/4*TAU+Math.PI/4; K.tube(V(Math.sin(a)*.6,2.6,Math.cos(a)*.6),V(Math.sin(a)*1.2,2.6,Math.cos(a)*1.2),.06,M.armour,6); const x=Math.sin(a)*3.25, z=Math.cos(a)*3.25; K.tube(V(Math.sin(a)*2.85,2.6,Math.cos(a)*2.85),V(x,2.5,z),.05,M.armour,6); podDuct(g,x,2.3,z,.36,o,L7(on,z>0)); }
  K.cyl(.6,2.2,10,M.armour,0,2.0,0); K.cyl(.62,.012,10,A,0,2.7,0); K.box(.5,.07,.02,M.lens,0,2.5,.6);
  if(o.cargo) for(const y of [1.2,1.7]) for(const x of [-.22,.22]) crateBox(K,.4,.4,.4,x,y,.62);
  if(!o.gearUp) for(let i=0;i<3;i++){ const a=i/3*TAU; legStrut(K,V(Math.sin(a)*.55,1.0,Math.cos(a)*.55),V(Math.sin(a)*1.1,.05,Math.cos(a)*1.1),.35); }
  K.light(M.white,0,3.12,0,.025,g,.35); return g; }

// 10 BALLOON TUG: a lift balloon on a short tether over a powered crane drone that does the steering
//    (real basis: Skylifter / tethered aerostat lifts)
function o7_10(o){ const g=new T.Group(), K=kit(g), on=o.on!==false, A=on?M.amber:M.amberDim;
  const lg=new T.LatheGeometry([[0,2.3],[1.1,2.45],[1.75,3.0],[1.8,3.5],[1.5,4.05],[.8,4.4],[0,4.5]].map(q=>new T.Vector2(q[0],q[1])),32); K.add(lg,M.env,0,0,0);
  const nb=new T.Mesh(new T.TorusGeometry(1.8,.012,6,64),A); nb.rotation.x=Math.PI/2; nb.position.y=3.3; g.add(nb);
  for(let i=0;i<6;i++){ const a=i/6*TAU; rig(K,V(Math.sin(a)*1.05,2.45,Math.cos(a)*1.05),V(Math.sin(a)*.5,1.75,Math.cos(a)*.5)); }
  K.plan(hexPts(.75),.25,M.armour,1.5); K.plan(hexPts(.6),.08,M.shell2,1.75);
  for(let i=0;i<6;i++){ const a=i/6*TAU+TAU/12; const x=Math.sin(a)*1.55, z=Math.cos(a)*1.55; K.tube(V(Math.sin(a)*.7,1.62,Math.cos(a)*.7),V(x,1.62,z),.05,M.armour,6); podDuct(g,x,1.45,z,.34,o,L7(on,z>0)); }
  if(o.cargo){ rig(K,V(0,1.5,0),V(0,1.1,0)); K.box(1.2,.06,1.2,M.anod,0,1.08,0); for(const x of [-.3,.3]) for(const z of [-.3,.3]) crateBox(K,.55,.55,.55,x,.78,z); }
  if(!o.gearUp) for(let i=0;i<3;i++){ const a=i/3*TAU; legStrut(K,V(Math.sin(a)*.65,1.5,Math.cos(a)*.65),V(Math.sin(a)*1.3,.05,Math.cos(a)*1.3),.35); }
  K.light(M.white,0,4.52,0,.03,g,.4); return g; }

// 11 SEGMENTED: four linked lift-cell segments on a flexible spine, a duct pair on every joint, cargo under the middle
function o7_11(o){ const g=new T.Group(), K=kit(g), on=o.on!==false, A=on?M.amber:M.amberDim;
  const Z=[2.4,.8,-.8,-2.4]; Z.forEach((z,i)=>{ const r=i===0?.95:i===3?.85:1.05; const e=K.add(new T.SphereGeometry(r,24,16),M.env,0,2.7,z); e.scale.set(1.15,.95,.85);
    const t2=new T.Mesh(new T.TorusGeometry(r*1.15,.04,6,32),M.armour); t2.rotation.x=Math.PI/2; t2.scale.set(1,.83,1); t2.position.set(0,2.7,z); g.add(t2); });
  K.box(.25,.25,6.0,M.armour,0,1.78,0); K.box(.26,.012,5.8,A,0,1.66,0).castShadow=false;
  for(const z of [1.6,0,-1.6]){ K.box(.6,.5,.4,M.shell2,0,2.05,z); for(const sd of [-1,1]){ K.tube(V(sd*.3,2.0,z),V(sd*1.7,2.2,z),.05,M.armour,6); podDuct(g,sd*2.0,1.95,z,.32,o,L7(on,z>0)); } }
  fin(K,[[-3.2,0],[-2.6,0],[-2.9,.7],[-3.25,.72]],0,3.4,0,0);
  gondola(K,.8,.45,.9,1.5,2.8,o,A); if(o.cargo){ K.box(1.4,.06,1.6,M.anod,0,1.12,0); for(const x of [-.35,.35]) for(const z of [-.4,.4]) crateBox(K,.55,.5,.6,x,.85,z); }
  if(!o.gearUp) for(const z of [-2.2,2.2]) legStrut(K,V(0,1.66,z),V(0,.05,z),.4);
  return g; }

// 12 ARMOURED CELLS: hard-shell hexagonal gas tanks on a strong-back frame, four armoured ducts, a drop-ship cargo bay
function o7_12(o){ const g=new T.Group(), K=kit(g), on=o.on!==false, A=on?M.amber:M.amberDim;
  for(const [x,y] of [[-.95,2.95],[.95,2.95],[0,3.6],[0,2.3]]){ const c=cbox(K,1.05,1.05,5.6,.28,M.armour,x,y,0); for(const z of [-2.0,-.7,.7,2.0]) K.box(1.07,1.07,.05,M.shell2,x,y,z); }
  for(const [x,y] of [[-.95,2.95],[.95,2.95],[0,3.6]]){ fus(K,[[2.8,.45],[3.15,.25],[3.3,.02]],1,1,y,M.armour).position.x=x; fus(K,[[-3.3,.02],[-3.15,.25],[-2.8,.45]],1,1,y,M.armour).position.x=x; }
  K.box(2.1,.012,5.4,A,0,2.95,0).castShadow=false; 
  for(const [x,z] of [[2.15,1.9],[-2.15,1.9],[2.15,-1.9],[-2.15,-1.9]]){ K.tube(V(Math.sign(x)*1.45,2.95,z),V(x,2.95,z),.07,M.armour,6); const dg=new T.Group(); dg.position.set(x+Math.sign(x)*.3,2.6,z); g.add(dg); armDuct(dg,.45,.5,o,x+z,L7(on,z>0)); }
  cbox(K,1.6,.95,3.4,.14,M.armour,0,1.3,.2); K.box(1.2,.07,.02,M.lens,0,1.5,1.905); for(const sd of [-1,1]) K.box(.006,.6,2.8,M.shell2,sd*.802,1.3,.1);
  if(o.cargo) for(const x of [-.3,.3]) for(const z of [-.8,0,.8]) crateBox(K,.5,.45,.6,x,.6,z);
  fin(K,[[-3.3,0],[-2.5,0],[-3.0,.8],[-3.35,.85]],0,4.1,0,0,.07);
  if(!o.gearUp) for(const [x,z] of [[.7,1.5],[-.7,1.5],[.7,-1.2],[-.7,-1.2]]) legStrut(K,V(x,.85,z),V(x*1.2,.05,z),.32);
  K.light(M.white,0,1.3,1.92,.025,g,.35); return g; }

// ===================================================================== T7 ROUND 2: HARD-HULL LIFT SHIPS (hydrogen cells inside the hull, internal hold)
// loft: chamfered-rectangle sections [z, w, h, c, yc] joined into a faceted hull, ends capped
function loft(K,secs,y,mat=M.armour,caps=[1,1]){ const ring=s=>{ const [z,w,h,c,yc=0]=s, a=w/2, b=h/2; return [[-a+c,-b],[a-c,-b],[a,-b+c],[a,b-c],[a-c,b],[-a+c,b],[-a,b-c],[-a,-b+c]].map(q=>V(q[0],q[1]+yc+y,z)); };
  const R=secs.map(ring), pos=[]; const tri=(p,q,r)=>pos.push(p.x,p.y,p.z,q.x,q.y,q.z,r.x,r.y,r.z);
  for(let i=0;i<R.length-1;i++) for(let k=0;k<8;k++){ const a=R[i][k], b=R[i][(k+1)%8], c=R[i+1][(k+1)%8], d=R[i+1][k]; tri(a,b,c); tri(a,c,d); }
  for(const [Rr,flip,on] of [[R[0],1,caps[0]],[R[R.length-1],0,caps[1]]]){ if(!on) continue; const cen=Rr.reduce((m,p)=>m.add(p),V(0,0,0)).multiplyScalar(1/8); for(let k=0;k<8;k++){ const a=Rr[k], b=Rr[(k+1)%8]; flip?tri(cen,b,a):tri(cen,a,b); } }
  const ge=new T.BufferGeometry(); ge.setAttribute('position',new T.Float32BufferAttribute(pos,3)); ge.computeVertexNormals();
  const uv=[]; for(let i=0;i<pos.length;i+=3) uv.push(pos[i]*.5+pos[i+2]*.5,pos[i+1]*.5); ge.setAttribute('uv',new T.Float32BufferAttribute(uv,2)); return K.add(ge,mat,0,0,0); }
function h2Hatch(K,x,y,z,ry,rz=0){ const g2=new T.Group(); g2.position.set(x,y,z); g2.rotation.set(0,ry,rz); K.add(new T.BoxGeometry(.001,.001,.001),M.black,0,0,0); return g2; }
function cellPanel(g,x,y,z,ry,w=.9,h=.5){ const p=new T.Group(); p.position.set(x,y,z); p.rotation.y=ry; g.add(p); const PK=kit(p); PK.box(w,h,.012,M.shell2,0,0,0); PK.box(w*.9,.04,.014,M.amberPaint,0,h/2-.06,0); PK.cyl(.04,.04,8,M.steel,w/2-.1,-h/2+.1,.02,Math.PI/2);
  const st=new T.Mesh(new T.PlaneGeometry(w*.6,h*.3),stencil('H2 CELL','#c9cdd1',192,48,'700 30px "DejaVu Sans Mono", monospace')); st.position.set(-.05,-.02,.008); p.add(st); }
function seamsZ(K,x,y,zs,h,face){ for(const z of zs) K.box(.006,h,.006,M.black,x,y,z); }
function ramp(g,w,l,y,z,open){ const rg=new T.Group(); rg.position.set(0,y,z); rg.rotation.x=open?-.9:0; g.add(rg); const RK=kit(rg); RK.box(w,.05,l,M.shell2,0,0,-l/2); for(let k=0;k<5;k++) RK.box(w*.9,.008,.04,M.amberPaint,0,.03,-.15-k*l/5.5); }
const T7L=(on,f)=>on?(f?M.white:M.amber):M.amberDim;

// 1 WEDGE HAULER: a long angular wedge, cells in the upper hull, hold below, ducts in four sponsons
function q1(o){ const g=new T.Group(), K=kit(g), {box,light}=K, on=o.on!==false, A=on?M.amber:M.amberDim;
  loft(K,[[-3.5,3.4,1.9,.35,.1],[-1.0,3.6,2.1,.4,.1],[1.5,3.2,1.8,.35,0],[3.1,1.6,.9,.2,-.25],[3.55,.5,.35,.1,-.35]],1.75);
  loft(K,[[-3.3,2.6,.3,.08],[2.6,2.4,.3,.08]],2.85,M.shell2);
  for(const sd of [-1,1]){ box(.012,.016,6.2,A,sd*1.80,1.45,-.3).castShadow=false; for(const z of [-2.6,-1.6,-.6,.4]) cellPanel(g,sd*1.805,2.05,z,sd*Math.PI/2);
    for(const z of [2.0,-2.6]){ const dg=new T.Group(); dg.position.set(sd*2.55,1.15,z); g.add(dg); armDuct(dg,.55,.6,o,sd+z,T7L(on,z>0)); K.tube(V(sd*1.8,1.5,z),V(sd*2.0,1.5,z),.12,M.armour,8); } }
  loft(K,[[2.9,1.2,.5,.12,-.25],[3.4,.4,.2,.05,-.33]],1.76,M.lens); ramp(g,1.6,1.4,.86,-3.5,o.cargo==='winch'); light(M.white,0,1.42,3.56,.03,g,.4); light(A,0,2.95,-3.4,.025,g,.35);
  if(!o.gearUp) for(const [x,z] of [[1.0,2.0],[-1.0,2.0],[1.1,-2.5],[-1.1,-2.5]]) legStrut(K,V(x,.85,z),V(x*1.15,.05,z),.45); return g; }

// 2 SKY BARGE: a long box hull like a container ship of the air, cells along the top deck, a stern ramp, six ducts in side pods
function q2(o){ const g=new T.Group(), K=kit(g), {box,light}=K, on=o.on!==false, A=on?M.amber:M.amberDim;
  loft(K,[[-3.4,3.0,2.2,.3],[2.6,3.0,2.2,.3],[3.3,2.2,1.6,.4,-.2],[3.6,1.2,.8,.3,-.4]],1.9);
  for(let i=0;i<5;i++){ const z=-2.8+i*1.15; loft(K,[[z-.5,2.6,.5,.15],[z+.5,2.6,.5,.15]],3.25,M.shell2); box(2.62,.012,.012,A,0,3.0,z+.5).castShadow=false; }
  loft(K,[[3.0,1.9,.6,.15,-.05],[3.45,1.0,.35,.1,-.25]],2.15,M.lens);
  for(const sd of [-1,1]){ box(.012,.016,6.4,A,sd*1.505,1.0,-.2).castShadow=false; seamsZ(K,sd*1.505,1.9,[-2.3,-1.15,0,1.15,2.3],2.0);
    for(const z of [2.2,0,-2.4]){ K.tube(V(sd*1.5,1.3,z),V(sd*2.2,1.3,z),.10,M.armour,8); const dg=new T.Group(); dg.position.set(sd*2.62,1.05,z); g.add(dg); armDuct(dg,.42,.5,o,sd+z,T7L(on,z>0)); } }
  ramp(g,2.0,1.6,.82,-3.4,o.cargo==='winch'); light(M.white,0,1.6,3.62,.03,g,.4);
  if(!o.gearUp) for(const [x,z] of [[1.1,2.4],[-1.1,2.4],[1.1,-2.6],[-1.1,-2.6]]) legStrut(K,V(x,.82,z),V(x,.05,z),.5); return g; }

// 3 FLYING-WING CARRIER: a thick angular blended wing, cells in the outer wings, hold in the centre body, eight fans in the wing
function q3(o){ const g=new T.Group(), K=kit(g), {box,light,plan}=K, on=o.on!==false, A=on?M.amber:M.amberDim;
  const OUT=[[0,3.6],[1.3,2.2],[3.3,-.5],[3.3,-1.4],[1.6,-1.6],[.9,-2.6],[-.9,-2.6],[-1.6,-1.6],[-3.3,-1.4],[-3.3,-.5],[-1.3,2.2]];
  const FANS=[[1.6,.35,.38],[-1.6,.35,.38],[2.55,-.75,.36],[-2.55,-.75,.36],[.95,-1.15,.33],[-.95,-1.15,.33]];
  slab(K,OUT,FANS.map(f=>[f[0],f[1],f[2]+.02]),1.4,.55,M.armour); slab(K,scl(OUT,.95),FANS.map(f=>[f[0],f[1],f[2]+.08]),1.95,.12,M.armour);
  loft(K,[[-2.4,1.5,1.4,.3,.2],[1.6,1.6,1.5,.35,.2],[3.1,.9,.8,.25,0],[3.55,.3,.3,.1,-.1]],1.7);
  loft(K,[[2.4,1.1,.5,.15,.5],[3.2,.5,.3,.1,.25]],1.7,M.lens);
  for(const [x,z,r] of FANS) fanHole(g,x,1.38,z,r,.58,T7L(on,z>0),o,x+z);
  for(const sd of [-1,1]){ edgeStrip(K,[sd*1.32,2.17],[sd*3.28,-.48],sd,1.68,A); cellPanel(g,sd*2.2,2.08,-.2,0,.8,.4); g.children[g.children.length-1].rotation.x=-Math.PI/2;
    fin(K,[[-2.5,0],[-1.6,0],[-2.1,.9],[-2.55,.95]],sd*.6,2.4,0,-sd*.35,.06); }
  ramp(g,1.2,1.3,1.0,-2.4,o.cargo==='winch'); light(M.white,0,1.6,3.58,.03,g,.4);
  if(!o.gearUp) for(const [x,z] of [[0,2.4],[1.1,-1.8],[-1.1,-1.8]]) legStrut(K,V(x,1.4,z),V(x*1.1,.05,z),.45); return g; }

// 4 TWIN-HULL: two angular hulls, cells in both, a bridge deck with the bridge pod, cargo carried inside the hulls, four ducts between
function q4(o){ const g=new T.Group(), K=kit(g), {box,light}=K, on=o.on!==false, A=on?M.amber:M.amberDim;
  for(const sd of [-1,1]){ const h=loft(K,[[-3.5,1.5,1.7,.3],[2.4,1.6,1.8,.35],[3.3,1.0,1.1,.3,-.25],[3.6,.4,.4,.1,-.4]],1.95); h.position.x=sd*1.9;
    for(const z of [-2.6,-1.5,-.4,.7]) cellPanel(g,sd*2.705,2.25,z,sd*Math.PI/2,.8,.45); box(.012,.016,5.8,A,sd*2.705,1.45,-.4).castShadow=false;
    const r=new T.Group(); r.position.x=sd*1.9; g.add(r); ramp(r,1.0,1.2,1.05,-3.5,o.cargo==='winch'); light(M.white,sd*1.9,1.5,3.62,.025,g,.35);
    if(!o.gearUp) for(const z of [2.3,-2.6]) legStrut(K,V(sd*1.9,1.05,z),V(sd*2.05,.05,z),.45); }
  loft(K,[[-.6,2.2,.45,.12],[2.0,2.2,.45,.12]],2.75); loft(K,[[1.4,1.0,.6,.15],[2.7,.8,.45,.12,-.05]],3.2); loft(K,[[2.3,.85,.3,.1,.05],[2.75,.6,.2,.06]],3.22,M.lens);
  for(const z of [-2.4,-1.0]){ box(2.2,.18,.25,M.armour,0,2.6,z); }
  for(const [x,z] of [[.75,-1.7],[-.75,-1.7]]){ const dg=new T.Group(); dg.position.set(x,1.9,z); g.add(dg); armDuct(dg,.55,.55,o,x,A); }
  for(const [x,z] of [[.75,1.0],[-.75,1.0]]){ const dg=new T.Group(); dg.position.set(x,1.9,z); g.add(dg); armDuct(dg,.55,.55,o,x+2,on?M.white:M.amberDim); }
  return g; }

// 5 MONITOR: a low, very wide armoured hull, cells in the side bulges, eight fans in the deck, a raised bridge forward
function q5(o){ const g=new T.Group(), K=kit(g), {box,light}=K, on=o.on!==false, A=on?M.amber:M.amberDim;
  const OUT=[[2.2,3.4],[3.2,2.4],[3.3,-2.6],[2.6,-3.4],[-2.6,-3.4],[-3.3,-2.6],[-3.2,2.4],[-2.2,3.4]];
  const FANS=[[2.2,1.4,.42],[-2.2,1.4,.42],[2.2,0,.42],[-2.2,0,.42],[2.2,-1.4,.42],[-2.2,-1.4,.42],[2.2,-2.6,.38],[-2.2,-2.6,.38]].filter((f,i)=>i<6);
  slab(K,OUT,FANS.map(f=>[f[0],f[1],f[2]+.02]),.75,.75,M.armour); slab(K,scl(OUT,.96),FANS.map(f=>[f[0],f[1],f[2]+.08]),1.5,.1,M.armour);
  for(const [x,z,r] of FANS) fanHole(g,x,.73,z,r,.78,T7L(on,z>0),o,x+z);
  loft(K,[[-2.8,2.4,1.1,.25],[1.2,2.4,1.1,.25],[2.2,1.8,.9,.25,-.1]],2.15); loft(K,[[1.6,1.9,.35,.1,.2],[2.25,1.4,.25,.08,.05]],2.15,M.lens);
  for(const sd of [-1,1]){ for(const z of [-2.2,-1.0,.2]) cellPanel(g,sd*1.205,2.15,z,sd*Math.PI/2,.8,.45); box(.012,.016,6.6,A,sd*3.31,1.2,-.1).castShadow=false; }
  ramp(g,1.8,1.3,.78,-3.4,o.cargo==='winch'); light(M.white,0,1.1,3.42,.03,g,.4);
  if(!o.gearUp) for(const [x,z] of [[1.0,2.6],[-1.0,2.6],[1.0,-2.8],[-1.0,-2.8]]) legStrut(K,V(x,.75,z),V(x*1.1,.05,z),.5); return g; }

// 6 SPINE CARRIER: a central spine carrying a stack of cargo modules, an armoured lift hull either side
function q6(o){ const g=new T.Group(), K=kit(g), {box,light}=K, on=o.on!==false, A=on?M.amber:M.amberDim;
  for(const sd of [-1,1]){ const h=loft(K,[[-3.4,1.3,1.6,.3],[2.4,1.4,1.7,.35],[3.4,.7,.9,.25,-.2],[3.7,.25,.3,.08,-.3]],2.35); h.position.x=sd*2.3;
    for(const z of [-2.4,-1.3,-.2,.9]) cellPanel(g,sd*3.005,2.6,z,sd*Math.PI/2,.8,.45); box(.012,.016,5.8,A,sd*3.005,1.8,-.4).castShadow=false;
    for(const z of [2.6,-2.9]){ const dg=new T.Group(); dg.position.set(sd*2.3,.95,z); g.add(dg); armDuct(dg,.45,.45,o,sd+z,T7L(on,z>0)); } light(M.white,sd*2.3,2.0,3.72,.025,g,.35); }
  loft(K,[[-3.3,.9,.6,.15],[3.3,.9,.6,.15],[3.8,.5,.4,.1]],3.0); for(const sd of [-1,1]) for(const z of [-2.4,0,2.4]) box(1.4,.2,.25,M.armour,sd*1.0,2.95,z);
  if(o.cargo) for(const z of [-2.0,-.65,.7,2.05]) cargoModule(K,1.3,1.1,1.2,0,2.0,z);
  if(!o.gearUp) for(const [x,z] of [[2.3,2.0],[-2.3,2.0],[2.3,-2.2],[-2.3,-2.2]]) legStrut(K,V(x,1.5,z),V(x,.05,z),.45); return g; }

// 7 FACETED WHALE: one big faceted lifting hull (hard panels, not fabric), chines, cells inside, hold and ramp below
function q7(o){ const g=new T.Group(), K=kit(g), {box,light}=K, on=o.on!==false, A=on?M.amber:M.amberDim;
  loft(K,[[-3.6,.8,.7,.2,.3],[-2.6,2.6,2.0,.7,.1],[-.5,3.4,2.5,.9],[1.6,3.2,2.3,.85],[3.0,2.0,1.5,.55,-.1],[3.7,.6,.5,.18,-.2]],2.2);
  loft(K,[[2.6,1.6,.6,.2,.35],[3.5,.6,.3,.1,.1]],2.2,M.lens);
  for(const sd of [-1,1]){ box(.012,.016,5.4,A,sd*1.71,2.2,-.3).castShadow=false; for(const z of [-1.6,-.4,.8]) cellPanel(g,sd*1.71,2.75,z,sd*Math.PI/2,.8,.45);
    for(const z of [1.6,-1.9]){ K.tube(V(sd*1.6,1.5,z),V(sd*2.3,1.4,z),.12,M.armour,8); const dg=new T.Group(); dg.position.set(sd*2.75,1.15,z); g.add(dg); armDuct(dg,.48,.55,o,sd+z,T7L(on,z>0)); } }
  fin(K,[[-3.6,0],[-2.4,0],[-3.1,1.0],[-3.65,1.05]],0,3.25,0,0,.08); for(const sd of [-1,1]) fin(K,[[-3.4,0],[-2.4,0],[-3.0,.8],[-3.45,.82]],sd*.4,2.2,0,-sd*1.3,.06);
  ramp(g,1.4,1.3,1.0,-2.6,o.cargo==='winch'); light(M.white,0,2.0,3.72,.03,g,.4);
  if(!o.gearUp) for(const [x,z] of [[1.0,1.8],[-1.0,1.8],[1.0,-1.8],[-1.0,-1.8]]) legStrut(K,V(x,1.0,z),V(x*1.2,.05,z),.45); return g; }

// 8 TRIDENT: three parallel angular hulls, the middle one longer with the bridge and hold, cells in the outer two
function q8(o){ const g=new T.Group(), K=kit(g), {box,light}=K, on=o.on!==false, A=on?M.amber:M.amberDim;
  const mid=loft(K,[[-3.6,1.6,1.7,.35],[2.6,1.6,1.7,.35],[3.5,.8,.9,.25,-.2],[3.8,.3,.3,.1,-.3]],1.75);
  loft(K,[[2.9,1.2,.5,.15,.1],[3.6,.5,.25,.08,-.1]],1.75,M.lens);
  for(const sd of [-1,1]){ const h=loft(K,[[-3.0,1.1,1.3,.3],[1.8,1.1,1.3,.3],[2.6,.6,.7,.2,-.1],[2.9,.2,.25,.06,-.2]],2.25); h.position.x=sd*2.05;
    for(const z of [-2.2,-1.1,0]) cellPanel(g,sd*2.605,2.4,z,sd*Math.PI/2,.8,.45); box(.012,.016,4.6,A,sd*2.605,1.75,-.6).castShadow=false;
    for(const z of [-2.0,0,1.6]) box(1.0,.25,.4,M.armour,sd*1.25,2.1,z);
    for(const z of [1.3,-2.6]){ const dg=new T.Group(); dg.position.set(sd*2.05,.95,z); g.add(dg); armDuct(dg,.45,.4,o,sd+z,T7L(on,z>0)); } }
  ramp(g,1.2,1.3,.92,-3.6,o.cargo==='winch'); light(M.white,0,1.5,3.82,.03,g,.4);
  if(!o.gearUp) for(const [x,z] of [[.6,2.3],[-.6,2.3],[.6,-2.8],[-.6,-2.8]]) legStrut(K,V(x,.92,z),V(x*1.2,.05,z),.45); return g; }

// 9 ARROWHEAD: a big swept delta with a raised hull, cells in the wing roots, four fans buried, two tall fins
function q9(o){ const g=new T.Group(), K=kit(g), {box,light}=K, on=o.on!==false, A=on?M.amber:M.amberDim;
  const OUT=[[0,3.7],[3.2,-2.4],[3.3,-3.2],[1.4,-2.8],[0,-3.4],[-1.4,-2.8],[-3.3,-3.2],[-3.2,-2.4]];
  const FANS=[[1.3,-.6,.42],[-1.3,-.6,.42],[2.3,-2.2,.38],[-2.3,-2.2,.38]];
  slab(K,OUT,FANS.map(f=>[f[0],f[1],f[2]+.02]),1.2,.4,M.armour); slab(K,scl(OUT,.95),FANS.map(f=>[f[0],f[1],f[2]+.08]),1.6,.1,M.armour);
  loft(K,[[-3.2,1.5,1.3,.3,.3],[1.6,1.4,1.2,.3,.25],[3.0,.7,.6,.2,0],[3.6,.2,.2,.05,-.1]],1.75); loft(K,[[2.0,1.0,.5,.15,.5],[3.0,.45,.28,.08,.2]],1.75,M.lens);
  for(const [x,z,r] of FANS) fanHole(g,x,1.18,z,r,.43,T7L(on,z>-1),o,x+z);
  for(const sd of [-1,1]){ edgeStrip(K,[sd*.05,3.65],[sd*3.18,-2.38],sd,1.4,A); fin(K,[[-3.3,0],[-2.0,0],[-2.8,1.3],[-3.35,1.35]],sd*.85,1.7,0,-sd*.15,.07); cellPanel(g,sd*.71,2.1,-1.2,sd*Math.PI/2,.8,.45); }
  ramp(g,1.2,1.2,1.12,-3.2,o.cargo==='winch'); light(M.white,0,1.42,3.72,.03,g,.4);
  if(!o.gearUp) for(const [x,z] of [[0,2.2],[1.0,-2.2],[-1.0,-2.2]]) legStrut(K,V(x,1.2,z),V(x,.05,z),.45); return g; }

// 10 HEAVY LANDER: a tall hexagonal hull on four legs, cells in the upper hull, hold in the lower deck, four ducts on stub pylons
function q10(o){ const g=new T.Group(), K=kit(g), {box,light,plan}=K, on=o.on!==false, A=on?M.amber:M.amberDim;
  plan(hexPts(2.0,TAU/12),1.6,M.armour,1.0); plan(hexPts(1.85,TAU/12),.9,M.armour,2.6); plan(hexPts(1.3,TAU/12),.35,M.shell2,3.5); plan(hexPts(.7,TAU/12),.2,M.armour,3.85);
  const b=plan(hexPts(2.012,TAU/12),.02,A,2.3); b.castShadow=false;
  for(let i=0;i<6;i++){ const t2=i/6*TAU, ap=1.85*Math.cos(Math.PI/6)+.004; cellPanel(g,Math.sin(t2)*ap,3.05,Math.cos(t2)*ap,t2,.9,.5); }
  for(let i=0;i<4;i++){ const a=Math.PI/4+i*Math.PI/2; K.tube(V(Math.sin(a)*1.7,2.2,Math.cos(a)*1.7),V(Math.sin(a)*2.5,2.2,Math.cos(a)*2.5),.12,M.armour,8); const dg=new T.Group(); dg.position.set(Math.sin(a)*2.85,1.95,Math.cos(a)*2.85); g.add(dg); armDuct(dg,.45,.5,o,i,T7L(on,Math.cos(a)>0)); }
  box(1.5,.25,.02,M.lens,0,3.2,1.6); const r=new T.Group(); r.position.z=-1.72; g.add(r); ramp(r,1.4,1.3,1.0,0,o.cargo==='winch');
  if(!o.gearUp) for(let i=0;i<4;i++){ const a=i*Math.PI/2; legStrut(K,V(Math.sin(a)*1.5,1.0,Math.cos(a)*1.5),V(Math.sin(a)*2.1,.05,Math.cos(a)*2.1),.55); }
  light(M.white,0,4.06,0,.03,g,.4); return g; }

// 11 HAMMERHEAD: a wide forward lift section holding the cells and four fans, a long cargo hull trailing behind
function q11(o){ const g=new T.Group(), K=kit(g), {box,light}=K, on=o.on!==false, A=on?M.amber:M.amberDim;
  const OUT=[[2.2,3.3],[3.3,2.6],[3.3,1.0],[2.2,.5],[-2.2,.5],[-3.3,1.0],[-3.3,2.6],[-2.2,3.3]]; const FANS=[[2.4,1.8,.42],[-2.4,1.8,.42],[1.2,1.9,.4],[-1.2,1.9,.4]];
  slab(K,OUT,FANS.map(f=>[f[0],f[1],f[2]+.02]),1.9,.7,M.armour); slab(K,scl(OUT,.96).map(q=>[q[0],q[1]+.08]),FANS.map(f=>[f[0],f[1],f[2]+.08]),2.6,.1,M.armour);
  for(const [x,z,r] of FANS) fanHole(g,x,1.88,z,r,.73,T7L(on,true),o,x);
  loft(K,[[-3.6,1.6,1.6,.35],[1.0,1.7,1.7,.4],[2.6,1.3,1.3,.35,.2]],1.7); loft(K,[[2.9,2.0,.35,.1],[3.32,1.6,.25,.08]],2.35,M.lens);
  for(const sd of [-1,1]){ for(const z of [-2.6,-1.5,-.4]) cellPanel(g,sd*.855,1.9,z,sd*Math.PI/2,.8,.45); box(.012,.016,4.4,A,sd*.855,1.2,-1.4).castShadow=false; edgeStrip(K,[sd*2.25,3.28],[sd*3.28,2.62],sd,2.25,A); }
  for(const sd of [-1,1]){ const dg=new T.Group(); dg.position.set(sd*1.35,1.0,-3.0); g.add(dg); armDuct(dg,.4,.45,o,sd*5,M.amber); K.tube(V(sd*.8,1.4,-3.0),V(sd*.95,1.4,-3.0),.1,M.armour,8); }
  ramp(g,1.2,1.2,.92,-3.6,o.cargo==='winch'); light(M.white,0,2.3,3.32,.03,g,.4);
  if(!o.gearUp) for(const [x,z] of [[2.0,2.2],[-2.0,2.2],[.7,-2.8],[-.7,-2.8]]) legStrut(K,V(x,1.0,z),V(x*1.05,.05,z),.45); return g; }

// 12 GANTRY SHIP: an exposed structural frame carrying sealed cell tanks above and the hold below, ducts at the corners
function q12(o){ const g=new T.Group(), K=kit(g), {box,light,tube}=K, on=o.on!==false, A=on?M.amber:M.amberDim;
  for(const sd of [-1,1]) for(const y of [1.2,3.3]) cbox(K,.22,.22,7.0,.05,M.armour,sd*1.55,y,0);
  for(const z of [-3.3,-1.65,0,1.65,3.3]) for(const sd of [-1,1]){ cbox(K,.18,2.3,.18,.04,M.armour,sd*1.55,2.25,z); } for(const z of [-3.3,0,3.3]) for(const y of [1.2,3.3]) cbox(K,3.1,.18,.18,.04,M.armour,0,y,z);
  for(const x of [-.75,.75]) for(const z of [-1.65,1.65]){ loft(K,[[z-1.5,1.3,1.1,.32],[z+1.5,1.3,1.1,.32]],2.75).position.x=x; cellPanel(g,x+Math.sign(x)*.652,2.75,z,Math.sign(x)*Math.PI/2,.8,.4); }
  loft(K,[[-3.2,2.8,.9,.2],[3.0,2.8,.9,.2],[3.5,2.0,.7,.2,-.05]],.72); loft(K,[[3.1,2.2,.3,.08],[3.52,1.6,.22,.06]],.80,M.lens);
  for(const sd of [-1,1]) box(.012,.016,6.2,A,sd*1.665,1.2,0).castShadow=false;
  for(const [x,z] of [[2.25,2.9],[-2.25,2.9],[2.25,-2.9],[-2.25,-2.9]]){ const dg=new T.Group(); dg.position.set(x,2.0,z); g.add(dg); armDuct(dg,.48,.5,o,x+z,T7L(on,z>0)); tube(V(Math.sign(x)*1.65,2.25,z),V(Math.sign(x)*1.72,2.25,z),.1,M.armour,8); }
  ramp(g,1.8,1.2,.28,-3.2,o.cargo==='winch');
  if(!o.gearUp) for(const [x,z] of [[1.55,3.3],[-1.55,3.3],[1.55,-3.3],[-1.55,-3.3]]) legStrut(K,V(x,1.1,z),V(x,.05,z),.45); light(M.white,0,3.45,3.4,.03,g,.4); return g; }

// ===================================================================== T7 SPINE CARRIER (Justin picked round-2 option 6)
// Two faceted armoured lift hulls, each holding hydrogen lift cells behind four access panels, joined by a central
// spine that carries four 8-block cargo modules in clamp cradles (32 blocks). A bridge block with a glass sensor face
// leads the spine; four armoured ducts gimbal under the hulls; canted tail fins; lit chines; o.cargo 'winch' lowers
// the modules on cables from the spine.
const mod8Tex=ctex(256,256,(x,w,h)=>{ x.fillStyle='#3a4046'; x.fillRect(0,0,w,h); for(let i=0;i<w;i+=12){ x.fillStyle='#2c3135'; x.fillRect(i,0,4,h); x.fillStyle='#4a5157'; x.fillRect(i+4,0,2,h); }
  x.fillStyle='#d9962f'; x.fillRect(0,104,w,48); x.fillStyle='#16181a'; x.font='700 30px "DejaVu Sans Mono", monospace'; x.textAlign='center'; x.fillText('CARGO 8',w/2,139); x.strokeStyle='#1f2326'; x.lineWidth=10; x.strokeRect(5,5,w-10,h-10); });
M.mod8=std({map:mod8Tex, roughness:.6, metalness:.3});
function cargoModule8(K,w,h,d,x,y,z){ K.box(w,h,d,M.mod8,x,y,z); for(const sx of [-1,1]) for(const sy of [-1,1]) for(const sz of [-1,1]) K.box(.10,.10,.10,M.anod,x+sx*(w/2-.04),y+sy*(h/2-.04),z+sz*(d/2-.04)); }
function t7(o){
  const g=new T.Group(), K=kit(g), {box,cyl,tube,light,add}=K, on=o.on!==false, A=on?M.amber:M.amberDim, tilt=(o.cruise||0)*.5;
  for(const sd of [-1,1]){ const hx=sd*2.3;
    const h=loft(K,[[-3.45,.9,1.1,.25,.05],[-3.0,1.3,1.6,.32],[2.0,1.4,1.7,.35],[2.9,1.15,1.3,.3,-.1],[3.45,.62,.62,.18,-.25],[3.72,.2,.2,.05,-.32]],2.35); h.position.x=hx;
    { const vz=new T.Group(); vz.position.set(hx,3.075,2.48); vz.rotation.x=Math.atan2(.4,.9); g.add(vz); const VK=kit(vz); VK.box(.82,.02,.78,M.lens,0,.012,0); VK.box(.86,.03,.04,M.shell2,0,.02,-.40); VK.box(.86,.03,.04,M.shell2,0,.02,.40); VK.box(.02,.03,.8,M.shell2,0,.025,0); }
    for(const s2 of [-1,1]) light(M.white,hx+s2*.5,2.15,3.05,.022,g,.3);
    const st=loft(K,[[-3.2,1.18,.12,.03],[2.0,1.25,.12,.03]],3.20,M.shell2); st.position.x=hx;
    // chine light lines, panel seams, H2 cell hatches on the outboard face, vents and a stencil
    box(.012,.016,5.6,A,hx+sd*.705,1.80,-.4).castShadow=false; box(.012,.016,5.0,A,hx-sd*.705,1.80,-.6).castShadow=false;
    for(const z of [-2.9,-1.8,-.7,.4,1.5]) box(.006,1.3,.006,M.black,hx+sd*.703,2.35,z);
    for(const z of [-2.35,-1.25,-.15,.95]) cellPanel(g,hx+sd*.705,2.55,z,sd*Math.PI/2,.8,.45);
    for(let k=0;k<6;k++) box(.008,.12,.03,M.black,hx-sd*.704,2.5,1.2+k*.08);
    { const s2=new T.Mesh(new T.PlaneGeometry(.5,.13),stencil('SC-T7')); s2.position.set(hx+sd*.706,1.98,1.7); s2.rotation.y=sd*Math.PI/2; g.add(s2); }
    // canted fins and aerials
    fin(K,[[-3.4,0],[-2.4,0],[-3.0,.85],[-3.45,.9]],hx+sd*.2,3.2,0,-sd*.35,.06); light(A,hx+sd*.5,4.0,-3.42,.02,g,.3);
    tube(V(hx,3.26,1.0),V(hx,3.6,1.0),.012,M.anod,6); light(M.white,hx,3.62,1.0,.016);
    // nose and tail lights, side nav
    light(M.white,hx,2.03,3.73,.028,g,.4); light(A,hx,2.35,-3.47,.024,g,.35); light(sd>0?M.white:A,hx+sd*.71,2.35,2.6,.02,g,.3);
    // two armoured ducts under each hull on gimbal yokes
    for(const z of [2.55,-2.75]){ const dg=new T.Group(); dg.position.set(hx,.92,z); dg.rotation.x=tilt; g.add(dg); armDuct(dg,.48,.45,o,sd+z,T7L(on,z>0));
      for(const s2 of [-1,1]) box(.08,.55,.12,M.shell2,hx+s2*.62,1.2,z); }
    // landing legs
    if(!o.gearUp) for(const z of [1.3,-1.5]) legStrut(K,V(hx,1.55,z),V(hx+sd*.25,.05,z),.45); }
  // spine: keel beam, cross-beams to the hulls, bridge block with glass face, lit centre line
  loft(K,[[-3.3,.95,.62,.15],[2.6,.95,.62,.15],[3.4,.65,.45,.12,-.05]],3.0);
  { const vz=new T.Group(); vz.position.set(0,3.27,3.0); vz.rotation.x=Math.atan2(.1,.8); g.add(vz); kit(vz).box(.6,.02,.6,M.lens,0,.012,0); }
  box(.02,.008,5.6,A,0,3.312,-.3).castShadow=false;
  for(const z of [-2.75,-1.35,0,1.35,2.6]) for(const sd of [-1,1]){ cbox(K,1.25,.22,.30,.05,M.armour,sd*1.05,2.95,z); box(1.2,.008,.02,M.black,sd*1.05,3.06,z+.15); }
  // module cradles: clamp arms down from the spine; modules (each 8 blocks) hang between the hulls
  const d=o.cargo==='winch'?(o.drop??0):0;
  for(const z of [-2.05,-.68,.69,2.06]){ for(const sx of [-1,1]){ tube(V(sx*.40,2.7,z-.5),V(sx*.62,2.40,z-.5),.03,M.steel,6); tube(V(sx*.40,2.7,z+.5),V(sx*.62,2.40,z+.5),.03,M.steel,6); box(.06,.18,.18,M.haz,sx*.66,2.32,z-.5); box(.06,.18,.18,M.haz,sx*.66,2.32,z+.5); }
    if(o.cargo){ const cg=new T.Group(); cg.position.y=-d; g.add(cg); cargoModule8(kit(cg),1.28,1.05,1.22,0,1.80,z);
      if(d>.02) for(const sx of [-1,1]) tube(V(sx*.45,2.68,z),V(sx*.45,2.35-d,z),.006,M.tether,4); } }
  light(A,0,2.68,0,.02,g,.28);
  return g; }

// ===================================================================== T8 OPTIONS: TWELVE ION-WIND CRAFT (EAD thrusters + ducted fans for VTOL, 32 blocks)
// electrohydrodynamic thruster: thin emitter wires (glowing faintly) ahead of thicker aerofoil collectors
const ionGlow=()=>new T.MeshBasicMaterial({color:NIGHT?0xfff0dc:0xd8d4cc, toneMapped:false});
function ionArray(par,span,rows,dy,on,chord=.22){ const K=kit(par), W=on?ionGlow():M.steel;
  for(let r=0;r<rows;r++){ const y=r*dy; K.cyl(.006,span,4,W,0,y,.0,0,0,Math.PI/2).castShadow=false; K.box(span,.022,chord,M.steel,0,y-.01,-.10-chord/2); }
  for(const sd of [-1,1]) K.box(.04,(rows-1)*dy+.12,chord+.18,M.armour,sd*(span/2+.02),(rows-1)*dy/2,-.06-chord/2); }
// a vertical ladder of electrodes in a frame, facing +z (thrust towards -z)
function ionPanel(g,x,y,z,span,rows,dy,on,ry=0){ const p=new T.Group(); p.position.set(x,y,z); p.rotation.y=ry; g.add(p); ionArray(p,span,rows,dy,on); return p; }
const T8L=(on,f)=>on?(f?M.white:M.amber):M.amberDim;

// 1 EAD BIPLANE: two long wings, three tiers of electrode ladders slung between them, slim hull with the hold, belly lift fans
function r1(o){ const g=new T.Group(), K=kit(g), {box,light}=K, on=o.on!==false, A=on?M.amber:M.amberDim;
  loft(K,[[-3.5,.3,.3,.08],[-2.8,.9,.9,.25],[2.4,1.1,1.1,.3],[3.3,.6,.6,.18,-.1],[3.6,.2,.2,.05,-.15]],1.55);
  for(const y of [1.25,3.05]){ cbox(K,6.8,.10,.95,.03,M.armour,0,y,.3); box(6.8,.012,.012,A,0,y,.78).castShadow=false; }
  for(const sd of [-1,1]){ cbox(K,.10,1.9,.95,.03,M.armour,sd*3.4,2.15,.3); for(const x of [1.1,2.25]) ionPanel(g,sd*x,1.45,.75,1.05,4,.42,on); light(A,sd*3.45,2.15,.3,.02,g,.3); }
  ionPanel(g,0,2.2,1.2,1.1,2,.42,on); cbox(K,.10,1.8,.6,.03,M.armour,0,2.15,.3);
  for(const z of [1.4,-1.8]) fanHole(g,0,1.25,z,.30,.6,T8L(on,z>0),o,z); loft(K,[[2.4,.6,.25,.08,.4],[3.2,.3,.15,.05,.2]],1.55,M.lens);
  fin(K,[[-3.5,0],[-2.6,0],[-3.1,1.0],[-3.55,1.05]],0,1.9,0,0,.06);
  if(!o.gearUp) for(const [x,z] of [[.4,1.6],[-.4,1.6],[.4,-2.2],[-.4,-2.2]]) legStrut(K,V(x,1.1,z),V(x*1.5,.05,z),.35); return g; }

// 2 ION BOX-WING: a closed rectangular wing frame filled with electrode ladders, the hull through the middle
function r2(o){ const g=new T.Group(), K=kit(g), {box,light}=K, on=o.on!==false, A=on?M.amber:M.amberDim;
  for(const y of [.9,3.4]) cbox(K,6.6,.14,1.2,.04,M.armour,0,y,0); for(const sd of [-1,1]) cbox(K,.14,2.64,1.2,.04,M.armour,sd*3.3,2.15,0);
  for(const x of [-2.4,-1.3,1.3,2.4]) ionPanel(g,x,1.15,.55,1.0,6,.4,on);
  loft(K,[[-3.4,.4,.4,.1],[-2.6,1.2,1.1,.3],[2.6,1.3,1.2,.35],[3.5,.5,.5,.15,-.1]],2.15); loft(K,[[2.6,.8,.3,.1,.5],[3.3,.4,.18,.06,.25]],2.15,M.lens);
  box(6.62,.012,.012,A,0,3.40,.61).castShadow=false;
  for(const z of [1.9,-1.9]){ const dg=new T.Group(); dg.position.set(0,.95,z); g.add(dg); armDuct(dg,.38,.4,o,z,T8L(on,z>0)); }
  if(!o.gearUp) for(const sd of [-1,1]) for(const z of [-.4,.4]) legStrut(K,V(sd*3.2,.85,z),V(sd*3.2,.05,z),.35); return g; }

// 3 ION DELTA: a sharp delta with banks of electrodes set into slots across the whole wing, two lift fans in the body
function r3(o){ const g=new T.Group(), K=kit(g), {box,light}=K, on=o.on!==false, A=on?M.amber:M.amberDim;
  const OUT=[[0,3.7],[3.3,-2.6],[3.3,-3.3],[-3.3,-3.3],[-3.3,-2.6]];
  slab(K,OUT,[[0,.8,.45],[0,-1.0,.45]],1.1,.35,M.armour); slab(K,scl(OUT,.93),[[0,.8,.52],[0,-1.0,.52]],1.45,.08,M.armour);
  for(const [x,z,w] of [[1.6,-1.4,1.5],[-1.6,-1.4,1.5],[2.3,-2.6,1.4],[-2.3,-2.6,1.4],[1.0,-2.6,1.0],[-1.0,-2.6,1.0]]){ ionPanel(g,x,1.62,z,w*.9,2,.16,on); }
  for(const z of [.8,-1.0]) fanHole(g,0,1.1,z,.45,.43,T8L(on,z>0),o,z);
  loft(K,[[1.6,.8,.35,.1,.15],[3.0,.4,.2,.06,.05]],1.55,M.lens);
  for(const sd of [-1,1]){ edgeStrip(K,[sd*.05,3.65],[sd*3.28,-2.58],sd,1.3,A); fin(K,[[-3.3,0],[-2.4,0],[-3.0,.9],[-3.35,.95]],sd*.9,1.53,0,-sd*.2,.05); }
  if(!o.gearUp) for(const [x,z] of [[0,2.0],[1.0,-2.4],[-1.0,-2.4]]) legStrut(K,V(x,1.1,z),V(x,.05,z),.4); return g; }

// 4 ION STACKS: a compact armoured hull with four tall electrode stacks on pylons (like engines), lift fans in the stacks' feet
function r4(o){ const g=new T.Group(), K=kit(g), {box,light}=K, on=o.on!==false, A=on?M.amber:M.amberDim;
  loft(K,[[-3.3,1.6,1.4,.35],[2.2,1.8,1.6,.4],[3.2,1.0,1.0,.3,-.15],[3.6,.3,.3,.08,-.25]],1.8); loft(K,[[2.4,1.2,.4,.12,.55],[3.1,.6,.22,.07,.3]],1.8,M.lens);
  for(const [sx,z] of [[1,1.5],[-1,1.5],[1,-2.0],[-1,-2.0]]){ K.tube(V(sx*.9,2.0,z),V(sx*2.1,2.0,z),.10,M.armour,8);
    cbox(K,1.3,2.6,1.0,.12,M.armour,sx*2.6,2.2,z); const p=ionPanel(g,sx*2.6,1.15,z+.52,1.0,6,.4,on); 
    const dg=new T.Group(); dg.position.set(sx*2.6,.5,z); g.add(dg); armDuct(dg,.32,.32,o,sx+z,T8L(on,z>0)); }
  for(const sd of [-1,1]) box(.012,.016,5.4,A,sd*.905,2.3,-.4).castShadow=false;
  if(!o.gearUp) for(const [x,z] of [[.6,1.4],[-.6,1.4],[.6,-1.8],[-.6,-1.8]]) legStrut(K,V(x,1.05,z),V(x*1.2,.05,z),.4); return g; }

// 5 LADDER GLIDER: a high-aspect wing on a slim hull, three-deep electrode ladders hanging under the wing, T-tail
function r5(o){ const g=new T.Group(), K=kit(g), {box,light}=K, on=o.on!==false, A=on?M.amber:M.amberDim;
  loft(K,[[-3.6,.3,.4,.1,.2],[-2.6,.8,.9,.25],[2.0,1.2,1.3,.35],[3.1,.7,.8,.22,-.1],[3.6,.2,.25,.06,-.2]],1.6); loft(K,[[2.1,.85,.35,.1,.55],[3.0,.45,.2,.06,.3]],1.6,M.lens);
  cbox(K,6.9,.14,1.1,.04,M.armour,0,2.35,.4); box(6.9,.012,.012,A,0,2.35,.96).castShadow=false;
  for(const x of [-2.6,-1.4,1.4,2.6]){ for(const z of [.45]) ionPanel(g,x,1.15,z+.4,1.0,3,.36,on); cbox(K,.06,.9,.8,.02,M.armour,x+(x>0?.52:-.52),1.85,.4); }
  cbox(K,.08,1.4,.8,.02,M.armour,0,2.3,-3.2); cbox(K,2.6,.08,.7,.02,M.armour,0,3.0,-3.25);
  for(const z of [1.2,-1.6]) fanHole(g,0,1.03,z,.32,.6,T8L(on,z>0),o,z);
  if(!o.gearUp) for(const [x,z] of [[.4,1.4],[-.4,1.4],[.4,-2.0],[-.4,-2.0]]) legStrut(K,V(x,1.0,z),V(x*1.5,.05,z),.35); return g; }

// 6 GILL MANTA: a wide flat lifting body, its trailing edge a deep louvred gill bank of electrodes, two big lift fans
function r6(o){ const g=new T.Group(), K=kit(g), {box,light}=K, on=o.on!==false, A=on?M.amber:M.amberDim;
  const OUT=[[0,3.4],[1.6,2.6],[3.3,.4],[3.3,-1.6],[-3.3,-1.6],[-3.3,.4],[-1.6,2.6]];
  slab(K,OUT,[[1.7,.5,.5],[-1.7,.5,.5]],1.0,.55,M.armour); slab(K,scl(OUT,.9),[[1.7,.5,.58],[-1.7,.5,.58]],1.55,.12,M.armour); loft(K,[[-1.4,1.6,.5,.15,.4],[2.0,1.4,.5,.15,.4],[3.0,.6,.3,.1,.25]],1.4);
  loft(K,[[2.0,1.0,.3,.08,.65],[2.9,.5,.2,.06,.45]],1.4,M.lens);
  for(const [x,z,r] of [[1.7,.5,.5],[-1.7,.5,.5]]) fanHole(g,x,.98,z,r,.6,T8L(on,true),o,x);
  for(let i=0;i<6;i++){ const x=-2.75+i*1.1; ionPanel(g,x,1.05,-1.62,1.0,3,.18,on); }
  for(const sd of [-1,1]) edgeStrip(K,[sd*1.62,2.58],[sd*3.28,.42],sd,1.28,A);
  if(!o.gearUp) for(const [x,z] of [[1.0,1.8],[-1.0,1.8],[1.4,-1.0],[-1.4,-1.0]]) legStrut(K,V(x,1.0,z),V(x*1.1,.05,z),.4); return g; }

// 7 TANDEM ION: two wings in tandem, each with an electrode bank under it, fans in the hull between them
function r7(o){ const g=new T.Group(), K=kit(g), {box,light}=K, on=o.on!==false, A=on?M.amber:M.amberDim;
  loft(K,[[-3.6,.3,.3,.08,.1],[-2.8,1.0,1.0,.28],[2.4,1.2,1.2,.33],[3.3,.6,.6,.18,-.1],[3.65,.2,.2,.05,-.15]],1.6); loft(K,[[2.5,.8,.3,.1,.5],[3.2,.4,.18,.06,.25]],1.6,M.lens);
  for(const [z,y,span] of [[1.6,1.25,5.6],[-2.2,2.4,6.8]]){ cbox(K,span,.12,1.0,.04,M.armour,0,y,z); box(span,.012,.012,A,0,y,z+.51).castShadow=false;
    for(const sd of [-1,1]) for(const x of [1.0,2.0].concat(span>6?[3.0]:[])) if(x<span/2) ionPanel(g,sd*x,y-.85,z+.5,.9,3,.25,on); }
  cbox(K,.10,1.0,.9,.03,M.armour,0,2.6,-2.6);
  for(const z of [0,-1.2,.9]) if(z!==.9) fanHole(g,0,1.0,z,.36,.6,T8L(on,z>-1),o,z);
  if(!o.gearUp) for(const [x,z] of [[.5,2.2],[-.5,2.2],[.5,-2.6],[-.5,-2.6]]) legStrut(K,V(x,1.0,z),V(x*1.5,.05,z),.35); return g; }

// 8 IONOCRAFT LIFTER: a big hexagonal ionocraft (wire over a foil skirt) that lifts itself with no moving parts,
//    the armoured cargo core in the middle, two small fans only for trim
function r8(o){ const g=new T.Group(), K=kit(g), {box,light,plan}=K, on=o.on!==false, A=on?M.amber:M.amberDim, W=on?ionGlow():M.steel;
  for(let i=0;i<6;i++){ const a0=i/6*TAU, a1=(i+1)/6*TAU, R=3.2, r=1.2; const p0=V(Math.sin(a0)*R,0,Math.cos(a0)*R), p1=V(Math.sin(a1)*R,0,Math.cos(a1)*R);
    for(const [rr,yy] of [[R,2.6],[r+.3,2.6]]){ const a=V(Math.sin(a0)*rr,yy,Math.cos(a0)*rr), b=V(Math.sin(a1)*rr,yy,Math.cos(a1)*rr); K.tube(a,b,.04,M.armour,6); }
    for(let k=0;k<5;k++){ const rr=1.6+k*.38; const a=V(Math.sin(a0)*rr,2.7,Math.cos(a0)*rr), b=V(Math.sin(a1)*rr,2.7,Math.cos(a1)*rr); K.tube(a,b,.006,W,4).castShadow=false;
      const m=K.add(new T.PlaneGeometry(a.distanceTo(b),.32),M.armourD,(a.x+b.x)/2,2.42,(a.z+b.z)/2); m.rotation.y=-(a0+a1)/2+Math.PI/2; }
    K.tube(V(Math.sin(a0)*1.5,2.6,Math.cos(a0)*1.5),V(Math.sin(a0)*R,2.6,Math.cos(a0)*R),.05,M.armour,6); light(i===0||i===5?M.white:A,Math.sin(a0)*R,2.6,Math.cos(a0)*R,.02,g,.3); }
  plan(hexPts(1.25),2.2,M.armour,.9); plan(hexPts(1.0),.25,M.shell2,3.1); plan(hexPts(1.27),.02,A,2.2); box(1.0,.12,.02,M.lens,0,2.75,1.09);
  for(const sd of [-1,1]){ const dg=new T.Group(); dg.position.set(sd*1.6,.75,0); g.add(dg); armDuct(dg,.25,.25,o,sd,A); }
  if(!o.gearUp) for(let i=0;i<3;i++){ const a=i/3*TAU; legStrut(K,V(Math.sin(a)*1.0,.95,Math.cos(a)*1.0),V(Math.sin(a)*1.7,.05,Math.cos(a)*1.7),.4); }
  light(M.white,0,3.4,0,.025,g,.35); return g; }

// 9 ION CATAMARAN: two armoured booms with electrode banks in their inner faces, a cargo hull slung between them
function r9(o){ const g=new T.Group(), K=kit(g), {box,light}=K, on=o.on!==false, A=on?M.amber:M.amberDim;
  for(const sd of [-1,1]){ const h=loft(K,[[-3.6,.3,.4,.08],[-2.8,.8,1.2,.2],[2.6,.8,1.2,.2],[3.6,.3,.4,.08]],2.4); h.position.x=sd*2.8;
    for(const z of [-1.9,-.6,.7,2.0]) ionPanel(g,sd*2.8,1.95,z+.55,.7,3,.3,on);
    const dg=new T.Group(); dg.position.set(sd*2.8,.95,0); g.add(dg); armDuct(dg,.38,.4,o,sd,A); light(M.white,sd*2.8,2.4,3.62,.02,g,.3); box(.012,.016,6.0,A,sd*(2.8+sd*.405),2.4,0).castShadow=false; }
  for(const z of [-2.4,0,2.4]) cbox(K,4.8,.2,.4,.05,M.armour,0,3.1,z);
  loft(K,[[-3.0,1.4,1.3,.3],[2.0,1.4,1.3,.3],[3.0,.7,.7,.2,-.15]],2.1); loft(K,[[2.1,1.0,.3,.1,.55],[2.9,.5,.2,.06,.25]],2.1,M.lens);
  if(!o.gearUp) for(const sd of [-1,1]) for(const z of [-2.4,2.4]) legStrut(K,V(sd*2.8,1.8,z),V(sd*2.9,.05,z),.35); return g; }

// 10 ION ARROW: a thick arrowhead hull with a row of tall electrode vanes along its back like a dorsal ridge
function r10(o){ const g=new T.Group(), K=kit(g), {box,light}=K, on=o.on!==false, A=on?M.amber:M.amberDim;
  const OUT=[[0,3.7],[2.6,-1.4],[3.1,-3.2],[0,-2.5],[-3.1,-3.2],[-2.6,-1.4]];
  slab(K,OUT,[[1.6,-1.6,.42],[-1.6,-1.6,.42]],.9,.55,M.armour); slab(K,scl(OUT,.85),[],1.45,.3,M.armour); slab(K,scl(OUT,.55),[],1.75,.15,M.shell2);
  loft(K,[[.8,1.0,.3,.1,.0],[2.4,.5,.2,.06,-.1]],1.95,M.lens);
  for(let i=0;i<5;i++){ const z=-1.6+i*.55; for(const sd of [-1,1]){ const p=ionPanel(g,sd*.6,1.95,z,.7,3,.25,on); p.rotation.z=-sd*.3; } }
  for(const [x,z,r] of [[1.6,-1.6,.42],[-1.6,-1.6,.42]]) fanHole(g,x,.88,z,r,.58,A,o,x);
  for(const sd of [-1,1]) edgeStrip(K,[sd*.05,3.65],[sd*2.58,-1.38],sd,1.18,A);
  if(!o.gearUp) for(const [x,z] of [[0,2.0],[1.2,-2.2],[-1.2,-2.2]]) legStrut(K,V(x,.9,z),V(x,.05,z),.4); return g; }

// 11 ION RING: an armoured ring wing round the hull, electrode rings lining its inside, four lift fans in the ring
function r11(o){ const g=new T.Group(), K=kit(g), {box,light}=K, on=o.on!==false, A=on?M.amber:M.amberDim, W=on?ionGlow():M.steel, cy=2.4;
  const rg=fus(K,[[-.9,2.3],[-.5,2.42],[.6,2.42],[1.0,2.32]],1,1,cy,M.armourD,16,0); fus(K,[[-.85,2.18],[.9,2.2]],1,1,cy,M.shell2,16,0);
  for(const z of [-.5,-.1,.3,.7]){ const t2=new T.Mesh(new T.TorusGeometry(2.1,.006,4,48),W); t2.position.set(0,cy,z); g.add(t2); const c=new T.Mesh(new T.TorusGeometry(1.95,.03,4,48),M.steel); c.position.set(0,cy,z-.15); g.add(c); }
  loft(K,[[-3.5,.3,.3,.08],[-2.6,1.2,1.2,.3],[2.4,1.3,1.3,.35],[3.4,.6,.6,.18,-.1]],cy); loft(K,[[2.5,.9,.3,.1,.55],[3.3,.4,.18,.06,.2]],cy,M.lens);
  for(let i=0;i<4;i++){ const a=Math.PI/4+i*Math.PI/2; K.tube(V(Math.sin(a)*.6,cy+Math.cos(a)*.6,.1),V(Math.sin(a)*2.15,cy+Math.cos(a)*2.15,.1),.06,M.armour,6); }
  for(const sd of [-1,1]){ const dg=new T.Group(); dg.position.set(sd*2.75,cy-.4,0); g.add(dg); armDuct(dg,.35,.4,o,sd,A); }
  const lp=new T.Mesh(new T.TorusGeometry(2.43,.012,6,64),A); lp.position.set(0,cy,1.0); g.add(lp);
  if(!o.gearUp) for(const [x,z] of [[1.4,1.0],[-1.4,1.0],[1.4,-.8],[-1.4,-.8]]) legStrut(K,V(x,cy-1.6,z),V(x*1.1,.05,z),.35); return g; }

// 12 ION SLAB: a flat rectangular armoured slab with the hold inside, every edge a louvred electrode bank, four corner fans
function r12(o){ const g=new T.Group(), K=kit(g), {box,light}=K, on=o.on!==false, A=on?M.amber:M.amberDim;
  const OUT=[[2.6,3.4],[3.2,2.8],[3.2,-3.0],[2.6,-3.5],[-2.6,-3.5],[-3.2,-3.0],[-3.2,2.8],[-2.6,3.4]];
  const FANS=[[2.2,2.2,.45],[-2.2,2.2,.45],[2.2,-2.4,.45],[-2.2,-2.4,.45]];
  slab(K,OUT,FANS.map(f=>[f[0],f[1],f[2]+.02]),.9,.8,M.armour); slab(K,scl(OUT,.94),FANS.map(f=>[f[0],f[1],f[2]+.08]),1.7,.1,M.armour);
  slab(K,[[1.2,2.4],[1.4,2.0],[1.4,-2.2],[-1.4,-2.2],[-1.4,2.0],[-1.2,2.4]],[],1.8,.35,M.armour); slab(K,[[.9,2.6],[1.15,2.2],[-1.15,2.2],[-.9,2.6]],[],1.8,.3,M.lens);
  for(const [x,z,r] of FANS) fanHole(g,x,.88,z,r,.83,T8L(on,z>0),o,x+z);
  for(let i=0;i<4;i++) ionPanel(g,-2.4+i*1.6,1.0,-3.5,1.4,4,.17,on,Math.PI);
  for(const sd of [-1,1]) for(let i=0;i<2;i++){ const p=ionPanel(g,sd*3.2,1.0,-.6+i*1.4,1.2,4,.17,on,sd*Math.PI/2); }
  for(const sd of [-1,1]) box(.012,.016,6.0,A,sd*3.205,1.55,-.1).castShadow=false;
  if(!o.gearUp) for(const [x,z] of [[1.0,2.6],[-1.0,2.6],[1.0,-2.8],[-1.0,-2.8]]) legStrut(K,V(x,.9,z),V(x,.05,z),.45); return g; }

// ===================================================================== T8 RING VARIATIONS (Justin liked option 11; no side fans: lift fans inside the hull)
// ion ring: a smooth annular wing (aerofoil section), electrode rings inside (glowing emitter wire ahead of a collector ring),
// lit lips front and back, support struts to the hull. Axis along z unless rotated.
function ionRing(g,R,L,on,A,o={}){ const rg=new T.Group(); g.add(rg); const K=kit(rg); const W=on?ionGlow():M.steel, th=o.th||.22, sy=o.sy||1;
  const sec=[[-L/2,R+.02],[-L/2+.06,R+th*.75],[-L/2+L*.3,R+th],[L/2-.1,R+th*.6],[L/2,R+.03],[L/2-.2,R-.01],[-L/2+L*.3,R-.03],[-L/2+.05,R-.01]];
  const ge=new T.LatheGeometry(sec.concat([sec[0]]).map(q=>new T.Vector2(q[1],q[0])),48); ge.rotateX(Math.PI/2); ge.scale(1,sy,1); K.add(ge,M.armourD,0,0,0);
  for(const [z,mat,r] of [[-L/2,A,R+.025],[L/2,on?M.white:M.amberDim,R+.035]]){ const t2=new T.Mesh(new T.TorusGeometry(r,.012,6,64),mat); t2.scale.set(1,sy,1); t2.position.z=z; rg.add(t2); }
  const n=o.stages||3; for(let i=0;i<n;i++){ const z=L/2-.25-i*(L-.5)/n; const w=new T.Mesh(new T.TorusGeometry(R-.14,.011,4,64),W); w.scale.set(1,sy,1); w.position.z=z; rg.add(w);
    const c=new T.Mesh(new T.TorusGeometry(R-.08,.035,4,64),M.steel); c.scale.set(1,sy,1); c.position.z=z-.16; rg.add(c); }
  for(let i=0;i<(o.panels||8);i++){ const a=i/(o.panels||8)*TAU; const m=K.box(.006,.05,L*.5,M.black,Math.sin(a)*(R+th*.95),Math.cos(a)*(R+th*.95)*sy,-.05); m.rotation.z=-a; }
  return rg; }
function strut(K,a,b,r=.06){ K.tube(a,b,r,M.armour,6); }
function sleekHull(K,len,w,h,y,glass=true){ const L2=len/2;
  loft(K,[[-L2,.3*w,.35*h,.08,.05*h],[-L2+.7,.8*w,.85*h,.22*w],[L2-1.6,w,h,.3*w],[L2-.6,.65*w,.65*h,.2*w,-.1*h],[L2,.15,.15,.04,-.2*h]],y);
  if(glass) loft(K,[[L2-1.45,.72*w,.3*h,.1,.42*h],[L2-.55,.42*w,.2*h,.06,.18*h]],y,M.lens); }
function bellyFans(g,y,zs,r,on,o){ for(const z of zs) fanHole(g,0,y,z,r,.45,on?M.white:M.amberDim,o,z); }

// V1 HALO: one big ring round a sleek hull; the lift fans sit in the hull's belly
function v1(o){ const g=new T.Group(), K=kit(g), on=o.on!==false, A=on?M.amber:M.amberDim, cy=2.6;
  sleekHull(K,7.2,1.55,1.35,cy); const r=ionRing(g,2.0,1.9,on,A); r.position.set(0,cy,.1);
  for(let i=0;i<4;i++){ const a=Math.PI/4+i*Math.PI/2; strut(K,V(Math.sin(a)*.6,cy+Math.cos(a)*.55,.1),V(Math.sin(a)*1.97,cy+Math.cos(a)*1.97,.1)); }
  K.box(.012,.016,6.0,A,0,cy+.68,-.3).castShadow=false; bellyFans(g,cy-.72,[2.0,-2.2],.40,on,o);
  fin(K,[[-3.6,0],[-2.8,0],[-3.3,.8],[-3.65,.85]],0,cy+.55,0,0,.05);
  if(!o.gearUp) for(const [x,z] of [[.5,1.2],[-.5,1.2],[.5,-1.6],[-.5,-1.6]]) legStrut(K,V(x,cy-.5,z),V(x*1.6,.05,z),.35);
  K.light(M.white,0,cy-.23,3.6,.025,g,.35); return g; }

// V2 TANDEM RINGS: two smaller rings, fore and aft, on one long hull
function v2(o){ const g=new T.Group(), K=kit(g), on=o.on!==false, A=on?M.amber:M.amberDim, cy=2.35;
  sleekHull(K,7.4,1.45,1.3,cy); for(const [z,R] of [[1.6,1.6],[-2.2,1.85]]){ const r=ionRing(g,R,1.3,on,A,{stages:2}); r.position.set(0,cy+(z<0?.2:0),z);
    for(let i=0;i<3;i++){ const a=i/3*TAU; strut(K,V(Math.sin(a)*.45,cy+Math.cos(a)*.42,z),V(Math.sin(a)*(R-.02),cy+(z<0?.2:0)+Math.cos(a)*(R-.02),z),.05); } }
  bellyFans(g,cy-.7,[.0],.4,on,o);
  if(!o.gearUp) for(const [x,z] of [[.5,.6],[-.5,.6],[.5,-.8],[-.5,-.8]]) legStrut(K,V(x,cy-.45,z),V(x*1.6,.05,z),.35);
  K.light(M.white,0,cy-.2,3.7,.025,g,.35); return g; }

// V3 FLATTENED RING: a wide elliptical ring like a closed wing, the hull slung inside it, cargo pod below
function v3(o){ const g=new T.Group(), K=kit(g), on=o.on!==false, A=on?M.amber:M.amberDim, cy=2.3;
  const r=ionRing(g,3.15,2.2,on,A,{sy:.55,th:.26,stages:3,panels:12}); r.position.set(0,cy,0);
  sleekHull(K,6.8,1.15,1.0,cy); for(const sd of [-1,1]){ strut(K,V(sd*.55,cy,.3),V(sd*3.1,cy,.3),.07); strut(K,V(sd*.55,cy,-.6),V(sd*3.1,cy,-.6),.07); }
  cbox(K,1.2,.7,2.6,.12,M.armour,0,cy-.95,-.2); K.box(.9,.06,.02,M.lens,0,cy-.85,1.105); bellyFans(g,cy-1.33,[.6,-1.0],.32,on,o);
  if(!o.gearUp) for(const [x,z] of [[.55,1.0],[-.55,1.0],[.55,-1.4],[-.55,-1.4]]) legStrut(K,V(x,cy-1.25,z),V(x*1.5,.05,z),.35);
  K.light(M.white,0,cy-.2,3.4,.025,g,.35); return g; }

// V4 HALO LIFTER: the ring lies flat round a disc-like hull and lifts straight up (an ionocraft halo); no fans needed
//    in the hover, two small belly fans for trim; drives forward by tilting the halo
function v4(o){ const g=new T.Group(), K=kit(g), on=o.on!==false, A=on?M.amber:M.amberDim, cy=2.3;
  const r=ionRing(g,2.9,.9,on,A,{stages:2,panels:16}); r.rotation.x=-Math.PI/2+(o.cruise?.18:0); r.position.set(0,cy,0);
  const lg=new T.LatheGeometry([[0,cy-.75],[1.0,cy-.7],[1.55,cy-.35],[1.65,cy],[1.4,cy+.35],[.8,cy+.6],[0,cy+.65]].map(q=>new T.Vector2(q[0],q[1])),8,Math.PI/8); K.add(lg,M.armour,0,0,0);
  K.cyl(.6,.25,8,M.shell2,0,cy+.72,0,0,Math.PI/8); K.box(1.0,.12,.03,M.lens,0,cy+.2,1.5); for(let i=0;i<6;i++){ const a=i/6*TAU; strut(K,V(Math.sin(a)*1.6,cy,Math.cos(a)*1.6),V(Math.sin(a)*2.78,cy,Math.cos(a)*2.78),.06); }
  const b=new T.Mesh(new T.TorusGeometry(1.66,.012,6,48),A); b.rotation.x=Math.PI/2; b.position.y=cy; g.add(b);
  for(const x of [-.6,.6]) fanHole(g,x,cy-.72,0,.3,.4,A,o,x);
  if(!o.gearUp) for(let i=0;i<3;i++){ const a=i/3*TAU; legStrut(K,V(Math.sin(a)*.9,cy-.7,Math.cos(a)*.9),V(Math.sin(a)*1.5,.05,Math.cos(a)*1.5),.38); }
  K.light(M.white,0,cy+.86,0,.025,g,.35); return g; }

// V5 CROSS RING: one ring braced by four electrode vanes in a cross through the middle, a short armoured hull at the hub
function v5(o){ const g=new T.Group(), K=kit(g), on=o.on!==false, A=on?M.amber:M.amberDim, cy=2.6;
  const r=ionRing(g,2.0,2.2,on,A,{stages:4}); r.position.set(0,cy,0);
  sleekHull(K,6.6,1.6,1.45,cy);
  for(let i=0;i<4;i++){ const a=i*Math.PI/2; const p=new T.Group(); p.position.set(0,cy,0); p.rotation.z=-a; g.add(p); const PK=kit(p);
    PK.box(.08,1.2,1.9,M.armour,0,1.35,0); for(let k=0;k<3;k++){ PK.box(.014,1.1,.008,on?ionGlow():M.steel,.05,1.35,.75-k*.6).castShadow=false; PK.box(.04,1.1,.10,M.steel,.06,1.35,.6-k*.6); } }
  bellyFans(g,cy-.78,[2.2,-2.3],.4,on,o);
  if(!o.gearUp) for(const [x,z] of [[.5,1.8],[-.5,1.8],[.5,-1.8],[-.5,-1.8]]) legStrut(K,V(x,cy-.6,z),V(x*1.6,.05,z),.35);
  K.light(M.white,0,cy-.26,3.2,.025,g,.35); return g; }

// V6 TRIPLE RING: three rings of rising size spaced down a long armoured hull, like stages of a thruster
function v6(o){ const g=new T.Group(), K=kit(g), on=o.on!==false, A=on?M.amber:M.amberDim, cy=2.55;
  sleekHull(K,7.4,1.45,1.3,cy);
  for(const [z,R] of [[2.0,1.4],[.1,1.7],[-1.9,2.0]]){ const r=ionRing(g,R,1.0,on,A,{stages:2,th:.18}); r.position.set(0,cy,z);
    for(let i=0;i<4;i++){ const a=Math.PI/4+i*Math.PI/2; strut(K,V(Math.sin(a)*.5,cy+Math.cos(a)*.48,z),V(Math.sin(a)*(R-.02),cy+Math.cos(a)*(R-.02),z),.045); } }
  bellyFans(g,cy-.7,[1.05,-1.0],.38,on,o);
  if(!o.gearUp) for(const [x,z] of [[.5,1.0],[-.5,1.0],[.5,-1.0],[-.5,-1.0]]) legStrut(K,V(x,cy-.5,z),V(x*1.6,.05,z),.35);
  K.light(M.white,0,cy-.22,3.7,.025,g,.35); return g; }

// ===================================================================== T8 HALO ION CARRIER (Justin picked variation 1, with better landing gear)
// A sleek armoured hull through one big ion ring (EAD thruster: four glowing emitter rings ahead of collector rings,
// lit lips, eight panel seams, four streamlined struts). Two lift fans under louvres in the belly for take-off and
// landing (no side fans). The 32-block hold sits in the hull behind a split belly door that lowers a cargo cradle.
// Landing gear: two main legs that fold out of armoured fairings on the ring's lower struts (oleo, torque link,
// pivoting hazard-edged foot) and a twin-wheel nose leg and tail leg out of hull bays with open doors.
function mainLeg(par,x,y,z,sd,up){ const g=new T.Group(); g.position.set(x,y,z); par.add(g); const K=kit(g);
  cbox(K,.34,.55,.62,.08,M.armour,0,-.1,0); K.box(.35,.012,.6,M.amberPaint,0,-.38,0);
  if(up) return g;
  const knee=V(sd*.35,-.95,0), foot=V(sd*.52,-y+.10,0);
  K.tube(V(0,-.3,0),knee,.07,M.armour,8); K.tube(knee,foot,.045,M.steel,10); K.tube(knee.clone().lerp(foot,.15),knee.clone().lerp(foot,.55),.065,M.anod,10);
  K.tube(V(0,-.35,.12),knee.clone().add(V(0,0,.12)).lerp(foot,.25),.018,M.steel,6); K.tube(V(0,-.35,-.12),knee.clone().add(V(0,0,-.12)).lerp(foot,.25),.018,M.steel,6);
  K.add(new T.SphereGeometry(.075,10,8),M.shell2,knee.x,knee.y,knee.z);
  const f=new T.Group(); f.position.copy(foot); g.add(f); const FK=kit(f); cbox(FK,.46,.08,.62,.03,M.armour,0,-.04,0); FK.box(.48,.03,.64,M.haz,0,-.085,0); FK.cyl(.06,.10,10,M.shell2,0,.03,0); return g; }
function wheelLeg(par,x,y,z,up){ const g=new T.Group(); g.position.set(x,y,z); par.add(g); const K=kit(g);
  K.box(.36,.02,.7,M.black,0,0,0); if(up) return g;
  for(const sd of [-1,1]){ const d=K.box(.02,.32,.62,M.armour,sd*.2,-.16,0); d.rotation.z=sd*.25; }
  cbox(K,.22,.45,.30,.05,M.armour,0,-.25,0); K.tube(V(0,-.4,0),V(0,-y+.38,0),.05,M.steel,10); K.tube(V(0,-.4,0),V(0,-y*.6,0),.08,M.anod,10); K.tube(V(0,-.05,-.32),V(0,-y*.6,-.02),.03,M.armour,8); K.tube(V(0,-y*.65,.06),V(0,-y+.42,.06),.02,M.steel,6); K.box(.36,.03,.3,M.anod,0,-y+.33,0);
  K.cyl(.025,.34,8,M.steel,0,-y+.16,0,0,0,Math.PI/2); for(const sd of [-1,1]){ K.cyl(.15,.10,16,M.rubber,sd*.12,-y+.16,0,0,0,Math.PI/2); K.cyl(.08,.105,12,M.anod,sd*.12,-y+.16,0,0,0,Math.PI/2); } return g; }
function t8(o){
  const g=new T.Group(), K=kit(g), {box,cyl,tube,light,add}=K, on=o.on!==false, A=on?M.amber:M.amberDim, cy=2.6, up=!!o.gearUp;
  sleekHull(K,7.2,1.55,1.35,cy);
  // hull detail: dorsal spine with a lit line, panel seams, intakes, stencil, strobe, aerials
  loft(K,[[-2.8,.5,.22,.06],[1.6,.5,.22,.06],[2.2,.25,.12,.04,-.04]],cy+.73,M.shell2); box(.012,.008,4.0,A,0,cy+.845,-.6).castShadow=false;
  for(const z of [-2.4,-1.3,1.3,2.2]) for(const sd of [-1,1]) box(.006,1.0,.006,M.black,sd*.777,cy,z);
  for(const sd of [-1,1]){ box(.006,.016,5.6,A,sd*.778,cy-.25,-.2).castShadow=false; for(let k=0;k<6;k++) box(.008,.12,.03,M.black,sd*.778,cy+.25,-2.2-k*.08);
    const st=new T.Mesh(new T.PlaneGeometry(.5,.13),stencil('IR-T8')); st.position.set(sd*.779,cy+.15,1.6); st.rotation.y=sd*Math.PI/2; g.add(st);
    light(sd>0?M.white:A,sd*.6,cy-.3,2.6,.02,g,.3); }
  tube(V(0,cy+.84,-1.8),V(0,cy+1.2,-1.8),.012,M.anod,6); light(M.white,0,cy+1.21,-1.8,.016); light(A,0,cy+.2,-3.6,.022,g,.32); light(M.white,0,cy-.23,3.6,.026,g,.35);
  { const fn=fin(K,[[-3.6,0],[-2.7,0],[-3.25,.75],[-3.65,.8]],0,cy+.6,0,0,.05); }
  // the ion ring and four struts with fairings
  const r=ionRing(g,2.0,1.9,on,A,{stages:4}); r.position.set(0,cy,.1);
  for(let i=0;i<4;i++){ const a=Math.PI/4+i*Math.PI/2; const s0=V(Math.sin(a)*.6,cy+Math.cos(a)*.55,.1), s1=V(Math.sin(a)*1.97,cy+Math.cos(a)*1.97,.1);
    const m=new T.Mesh(new T.BoxGeometry(.10,s0.distanceTo(s1),.55),M.armour); m.position.copy(s0).add(s1).multiplyScalar(.5); m.quaternion.setFromUnitVectors(V(0,1,0),s1.clone().sub(s0).normalize()); m.castShadow=true; g.add(m);
    const ln=new T.Mesh(new T.BoxGeometry(.02,s0.distanceTo(s1)*.8,.012),A); ln.position.copy(m.position).add(V(0,0,.276)); ln.quaternion.copy(m.quaternion); g.add(ln); }
  // belly: two lift fans under louvres, the split cargo door and cradle
  for(const z of [2.0,-2.2]){ fanHole(g,0,cy-.72,z,.40,.45,on?(z>0?M.white:M.amber):M.amberDim,o,z); for(let k=-3;k<=3;k++) box(.02,.03,2*Math.sqrt(Math.max(0,.4*.4-(k*.1)**2)),M.shell2,k*.1,cy-.73,z); }
  const open=o.cargo==='winch', d=open?(o.drop??.6):0;
  if(open){ for(const sd of [-1,1]){ const hp=new T.Group(); hp.position.set(sd*.6,cy-.67,.0); hp.rotation.z=sd*1.3; g.add(hp); kit(hp).box(.6,.03,2.6,M.armour,-sd*.3,0,0); }
    const cr=new T.Group(); cr.position.y=-d; g.add(cr); const CK=kit(cr); CK.box(1.15,.05,2.6,M.anod,0,cy-.7,0); CK.box(1.16,.008,2.6,A,0,cy-.73,0);
    for(const x of [-.29,.29]) for(const z of [-.97,-.32,.32,.97]) crateBox(CK,.52,.5,.6,x,cy-.42,z);
    for(const x of [-.5,.5]) for(const z of [-1.2,1.2]) tube(V(x,cy-.66,z),V(x,cy-.66-d,z),.006,M.tether,4); }
  else { box(.006,.01,2.6,M.black,0,cy-.675,0); for(const sd of [-1,1]) box(1.2,.01,.006,M.black,0,cy-.675,sd*1.3); }
  // landing gear
  for(const sd of [-1,1]) mainLeg(g,sd*1.32,cy-1.32,.1,sd,up);
  wheelLeg(g,0,cy-.68,2.85,up); wheelLeg(g,0,cy-.62,-2.95,up);
  return g; }

// ===================================================================== T9 OPTIONS: TWELVE SUPERCONDUCTING LIFTERS (six SC lift fans, 64 blocks = eight 8-block modules)
// superconducting lift fan: armoured duct with a cryostat jacket round it, a glowing YBCO coil band, three Stirling cryocooler heads
function scFan(g,x,y,z,R,o,lit,k=0){ const fg=new T.Group(); fg.position.set(x,y,z); g.add(fg); const K=kit(fg); const H=R*.75;
  const sh=new T.Mesh(new T.CylinderGeometry(R+.16,R+.16,H,12,1,true),M.armourD); sh.position.y=H/2; sh.castShadow=true; fg.add(sh);
  ring(K,0,H,0,R+.01,R+.17,.04,M.shell2); ring(K,0,-.04,0,R+.01,R+.17,.04,M.shell2);
  const cb=new T.Mesh(new T.TorusGeometry(R+.165,.025,6,48),lit); cb.rotation.x=Math.PI/2; cb.position.y=H*.55; fg.add(cb);
  const ln=new T.Mesh(new T.CylinderGeometry(R+.012,R+.012,H,32,1,true),M.duct); ln.position.y=H/2; fg.add(ln);
  for(let i=0;i<3;i++){ const a=i/3*TAU+k; const cx=Math.sin(a)*(R+.17), cz=Math.cos(a)*(R+.17); K.cyl(.07,.22,10,M.steel,cx,H+.11,cz); for(let f=0;f<4;f++) K.cyl(.09,.012,10,M.anod,cx,H+.04+f*.05,cz); K.cyl(.05,.05,8,M.shell2,cx,H+.25,cz); }
  fanCore(fg,R,H*.95,o,k); return fg; }
const L9=(on,f)=>on?(f?M.white:M.amber):M.amberDim;
function modules(K,nx,nz,y,z0=0,w=1.0,d=1.0,h=.9,gap=.06){ for(let i=0;i<nx;i++) for(let j=0;j<nz;j++) cargoModule8(K,w,h,d,(i-(nx-1)/2)*(w+gap),y,z0+(j-(nz-1)/2)*(d+gap)); }

// 1 HEX CARRIER: a hexagonal armoured hull, an SC fan at every corner, eight modules in the hold, a bridge crown
function s1(o){ const g=new T.Group(), K=kit(g), {box,light,plan}=K, on=o.on!==false, A=on?M.amber:M.amberDim;
  const P=hexPts(3.0,TAU/12), F=hexPts(2.35,TAU/12);
  slab(K,P,F.map(f=>[f[0],f[1],.76]),1.0,.9,M.armour); slab(K,scl(P,.92),F.map(f=>[f[0],f[1],.80]),1.9,.12,M.armour);
  F.forEach((f,i)=>scFan(g,f[0],1.25,f[1],.56,o,L9(on,f[1]>0),i));
  plan(hexPts(1.2,TAU/12),.45,M.armour,2.02); plan(hexPts(.9,TAU/12),.12,M.lens,2.47); plan(hexPts(3.02,TAU/12),.02,A,1.45);
  if(!o.gearUp) for(let i=0;i<3;i++){ const a=i/3*TAU+Math.PI/6; legStrut(K,V(Math.sin(a)*1.3,1.0,Math.cos(a)*1.3),V(Math.sin(a)*1.7,.05,Math.cos(a)*1.7),.5); }
  light(M.white,0,2.6,0,.03,g,.4); return g; }

// 2 RING LIFTER: a flat armoured ring with six SC fans set in it, the cargo pod of eight modules slung in the middle
function s2(o){ const g=new T.Group(), K=kit(g), {box,light,plan}=K, on=o.on!==false, A=on?M.amber:M.amberDim;
  const R0=1.55,R1=3.3; const sh=new T.Shape(); sh.absarc(0,0,R1,0,TAU,false); const hh=new T.Path(); hh.absarc(0,0,R0,0,TAU,true); sh.holes.push(hh);
  for(let i=0;i<6;i++){ const a=i/6*TAU+TAU/12; const hp=new T.Path(); hp.absarc(Math.sin(a)*2.42,-Math.cos(a)*2.42,.62,0,TAU,true); sh.holes.push(hp); }
  const ge=new T.ExtrudeGeometry(sh,{depth:.7,bevelEnabled:false,curveSegments:40}); ge.rotateX(-Math.PI/2); K.add(ge,M.armour,0,2.4,0);
  for(let i=0;i<6;i++){ const a=i/6*TAU+TAU/12; scFan(g,Math.sin(a)*2.42,2.35,Math.cos(a)*2.42,.58,o,L9(on,Math.cos(a)>0),i); }
  const t2=new T.Mesh(new T.TorusGeometry(R1+.01,.014,6,72),A); t2.rotation.x=Math.PI/2; t2.position.y=2.75; g.add(t2);
  for(let i=0;i<4;i++){ const a=i/4*TAU; K.tube(V(Math.sin(a)*1.55,2.75,Math.cos(a)*1.55),V(Math.sin(a)*.95,2.9,Math.cos(a)*.95),.07,M.armour,6); }
  plan(hexPts(1.05),.5,M.armour,2.7); plan(hexPts(.7),.12,M.lens,3.2);
  modules(K,2,2,1.85,0,1.0,1.0,.9); modules(K,2,2,.9,0,1.0,1.0,.9);
  if(!o.gearUp) for(let i=0;i<4;i++){ const a=Math.PI/4+i*Math.PI/2; legStrut(K,V(Math.sin(a)*2.8,2.4,Math.cos(a)*2.8),V(Math.sin(a)*3.1,.05,Math.cos(a)*3.1),.5); }
  light(M.white,0,3.33,0,.03,g,.4); return g; }

// 3 TRIMARAN: a central cargo hull and two outrigger hulls, three SC fans in each outrigger
function s3(o){ const g=new T.Group(), K=kit(g), {box,light}=K, on=o.on!==false, A=on?M.amber:M.amberDim;
  loft(K,[[-3.6,1.6,1.6,.35],[2.4,1.8,1.8,.4],[3.4,1.0,1.0,.3,-.2],[3.75,.3,.3,.08,-.3]],1.75); loft(K,[[2.6,1.2,.4,.12,.6],[3.4,.6,.22,.07,.3]],1.75,M.lens);
  for(const sd of [-1,1]){ const sp=[[-3.4,.8],[-2.9,1.4],[2.8,1.4],[3.4,.8]]; slab(K,[[-.7,3.3],[.7,3.3],[.7,-3.3],[-.7,-3.3]].map(q=>[q[0]+sd*2.75,q[1]]),[-2.1,0,2.1].map(z=>[sd*2.75,z,.55]),1.2,.75,M.armour);
    [-2.1,0,2.1].forEach((z,i)=>scFan(g,sd*2.75,1.15,z,.52,o,L9(on,z>1),i+sd)); box(.012,.016,6.4,A,sd*(2.75+sd*.705),1.55,0).castShadow=false;
    for(const z of [-2.6,0,2.6]) cbox(K,1.3,.22,.35,.05,M.armour,sd*1.55,2.0,z);
    if(!o.gearUp) for(const z of [-2.6,2.6]) legStrut(K,V(sd*2.75,1.2,z),V(sd*2.8,.05,z),.45); }
  light(M.white,0,1.5,3.77,.03,g,.4); return g; }

// 4 HEAVY WEDGE: a long armoured wedge, six SC fans in two rows down the deck, hold below, twin fins
function s4(o){ const g=new T.Group(), K=kit(g), {box,light}=K, on=o.on!==false, A=on?M.amber:M.amberDim;
  const OUT=[[0,3.75],[2.2,1.8],[3.2,-2.6],[2.6,-3.6],[-2.6,-3.6],[-3.2,-2.6],[-2.2,1.8]]; const FAN=[[1.7,.6],[-1.7,.6],[2.0,-1.0],[-2.0,-1.0],[2.1,-2.55],[-2.1,-2.55]];
  slab(K,OUT,FAN.map(f=>[f[0],f[1],.6]),.85,.9,M.armour); slab(K,scl(OUT,.93),FAN.map(f=>[f[0],f[1],.7]),1.75,.12,M.armour);
  loft(K,[[-3.4,1.6,.7,.2,.2],[1.6,1.5,.7,.2,.2],[3.0,.7,.4,.12,0]],1.95); loft(K,[[1.9,1.1,.25,.08,.42],[2.9,.5,.15,.05,.2]],1.95,M.lens);
  FAN.forEach((f,i)=>scFan(g,f[0],.88,f[1],.52,o,L9(on,f[1]>0),i));
  for(const sd of [-1,1]){ edgeStrip(K,[sd*.05,3.7],[sd*2.18,1.78],sd,1.3,A); fin(K,[[-3.5,0],[-2.5,0],[-3.1,1.0],[-3.55,1.05]],sd*.7,2.2,0,-sd*.2,.06); }
  if(!o.gearUp) for(const [x,z] of [[0,2.4],[.9,-2.8],[-.9,-2.8]]) legStrut(K,V(x,.85,z),V(x,.05,z),.5); return g; }

// 5 GANTRY LIFTER: a rectangular armoured frame with three SC fan pods down each side, a stack of eight modules inside
function s5(o){ const g=new T.Group(), K=kit(g), {box,light}=K, on=o.on!==false, A=on?M.amber:M.amberDim;
  for(const sd of [-1,1]){ cbox(K,.5,.5,7.2,.12,M.armour,sd*1.45,3.4,0); cbox(K,.4,.4,7.0,.1,M.armour,sd*1.45,1.0,0); box(.012,.016,6.8,A,sd*(1.45+sd*.255),3.4,0).castShadow=false;
    for(const z of [-3.3,-1.1,1.1,3.3]) cbox(K,.3,2.4,.3,.06,M.armour,sd*1.45,2.2,z);
    for(const z of [-2.3,0,2.3]){ K.tube(V(sd*1.7,3.2,z),V(sd*2.2,3.2,z),.12,M.armour,8); scFan(g,sd*2.75,2.9,z,.52,o,L9(on,z>1),z+sd); } }
  for(const z of [-3.3,3.3]) cbox(K,3.4,.5,.5,.12,M.armour,0,3.4,z);
  const br=new T.Group(); br.position.set(0,3.65,3.0); g.add(br); kit(br).prof([[0,0],[.65,0],[.5,.35],[0,.38]],1.6,M.armour); kit(br).prof([[.5,.08],[.64,.04],[.5,.3],[.42,.32]],1.3,M.lens);
  modules(K,2,4,1.75,-.3,1.05,1.3,.95); light(M.white,0,3.8,3.7,.03,g,.4);
  if(!o.gearUp) for(const sd of [-1,1]) for(const z of [-3.3,3.3]) legStrut(K,V(sd*1.45,.85,z),V(sd*1.55,.05,z),.45); return g; }

// 6 DELTA FLAGSHIP: a big arrowhead with six SC fans buried in it, a raised spine bridge, hold doors underneath
function s6(o){ const g=new T.Group(), K=kit(g), {box,light}=K, on=o.on!==false, A=on?M.amber:M.amberDim;
  const OUT=[[0,3.8],[3.4,-2.8],[3.4,-3.6],[-3.4,-3.6],[-3.4,-2.8]]; const FAN=[[.9,.4],[-.9,.4],[1.5,-1.4],[-1.5,-1.4],[2.5,-2.85],[-2.5,-2.85]];
  slab(K,OUT,FAN.map(f=>[f[0],f[1],f[0]>2?.48:.55]),.9,.8,M.armour); slab(K,scl(OUT,.93),FAN.map(f=>[f[0],f[1],.65]),1.7,.12,M.armour);
  FAN.forEach((f,i)=>scFan(g,f[0],.92,f[1],Math.abs(f[0])>2?.45:.52,o,L9(on,f[1]>0),i));
  loft(K,[[-3.4,.9,.8,.22,.2],[1.8,1.0,.8,.22,.2],[3.2,.4,.4,.1,0]],2.0); loft(K,[[2.0,.75,.3,.08,.45],[3.0,.3,.18,.05,.15]],2.0,M.lens);
  for(const sd of [-1,1]){ edgeStrip(K,[sd*.05,3.75],[sd*3.38,-2.78],sd,1.3,A); fin(K,[[-3.6,0],[-2.6,0],[-3.2,1.1],[-3.65,1.15]],sd*.45,2.35,0,-sd*.18,.06); }
  if(!o.gearUp) for(const [x,z] of [[0,2.4],[1.1,-2.4],[-1.1,-2.4]]) legStrut(K,V(x,.9,z),V(x,.05,z),.5); return g; }

// 7 TWIN-HULL: two armoured hulls with three SC fans down each, the module deck between them
function s7(o){ const g=new T.Group(), K=kit(g), {box,light}=K, on=o.on!==false, A=on?M.amber:M.amberDim;
  for(const sd of [-1,1]){ const hx=sd*2.35; const h=loft(K,[[-3.6,1.6,1.1,.3],[2.6,1.7,1.2,.32],[3.5,.9,.7,.22,-.1],[3.8,.3,.3,.08,-.2]],1.45); h.position.x=hx;
    for(const z of [-2.2,0,2.2]){ scFan(g,hx,1.38,z,.55,o,L9(on,z>1),z+sd); }
    const gl=loft(K,[[2.8,1.1,.25,.08,.45],[3.5,.5,.15,.05,.2]],1.45,M.lens); gl.position.x=hx; box(.012,.016,6.8,A,hx+sd*.86,1.45,0).castShadow=false;
    if(!o.gearUp) for(const z of [-2.8,2.8]) legStrut(K,V(hx,.9,z),V(hx+sd*.2,.05,z),.45); }
  cbox(K,3.0,.25,6.0,.06,M.armour,0,2.35,0); modules(K,2,4,1.75,0,1.05,1.3,.95); light(M.white,0,2.5,3.05,.025,g,.35); return g; }

// 8 SPIDER: a central armoured core, six SC fans on long articulated arms (folded down when parked), cargo slung below
function s8(o){ const g=new T.Group(), K=kit(g), {box,light,plan}=K, on=o.on!==false, A=on?M.amber:M.amberDim;
  plan(hexPts(1.4),1.4,M.armour,2.0); plan(hexPts(1.0),.3,M.shell2,3.4); plan(hexPts(.6),.15,M.lens,3.7); plan(hexPts(1.42),.02,A,2.7);
  for(let i=0;i<6;i++){ const a=i/6*TAU+TAU/12, r1=2.75; const s0=V(Math.sin(a)*1.25,2.8,Math.cos(a)*1.25), s1=V(Math.sin(a)*2.0,3.2,Math.cos(a)*2.0), s2=V(Math.sin(a)*r1,3.0,Math.cos(a)*r1);
    K.tube(s0,s1,.10,M.armour,6); K.tube(s1,s2,.08,M.armour,6); K.add(new T.SphereGeometry(.13,10,8),M.shell2,s1.x,s1.y,s1.z); scFan(g,s2.x,2.75,s2.z,.55,o,L9(on,Math.cos(a)>0),i); }
  modules(K,2,2,1.35,0,1.0,1.0,.85); modules(K,2,2,.45,0,1.0,1.0,.85);
  if(!o.gearUp) for(let i=0;i<3;i++){ const a=i/3*TAU; legStrut(K,V(Math.sin(a)*1.2,2.0,Math.cos(a)*1.2),V(Math.sin(a)*1.9,.05,Math.cos(a)*1.9),.45); }
  light(M.white,0,3.86,0,.03,g,.4); return g; }

// 9 SLAB CARRIER: a low rectangular armoured slab with six SC fans along its sides, the hold in the middle, a bridge tower
function s9(o){ const g=new T.Group(), K=kit(g), {box,light}=K, on=o.on!==false, A=on?M.amber:M.amberDim;
  const OUT=[[2.6,3.7],[3.3,3.0],[3.3,-3.2],[2.7,-3.7],[-2.7,-3.7],[-3.3,-3.2],[-3.3,3.0],[-2.6,3.7]]; const FAN=[[2.35,2.2],[-2.35,2.2],[2.35,0],[-2.35,0],[2.35,-2.3],[-2.35,-2.3]];
  slab(K,OUT,FAN.map(f=>[f[0],f[1],.62]),.9,1.0,M.armour); slab(K,scl(OUT,.95),FAN.map(f=>[f[0],f[1],.72]),1.9,.12,M.armour);
  FAN.forEach((f,i)=>scFan(g,f[0],.95,f[1],.56,o,L9(on,f[1]>1),i));
  slab(K,[[1.2,3.0],[1.4,2.4],[1.4,-3.0],[-1.4,-3.0],[-1.4,2.4],[-1.2,3.0]],[],2.0,.15,M.shell2); for(const z of [-2.2,-.8,.6,2.0]) box(2.8,.01,.006,M.black,0,2.16,z);
  const br=new T.Group(); br.position.set(0,2.0,-2.6); g.add(br); const BK=kit(br); cbox(BK,1.4,1.0,.9,.15,M.armour,0,.5,0); BK.box(1.1,.2,.02,M.lens,0,.65,.455); light(M.white,0,3.05,-2.6,.025,g,.35);
  for(const sd of [-1,1]) box(.012,.016,6.6,A,sd*3.305,1.4,-.1).castShadow=false;
  if(!o.gearUp) for(const [x,z] of [[1.2,3.0],[-1.2,3.0],[1.2,-3.1],[-1.2,-3.1]]) legStrut(K,V(x,.9,z),V(x,.05,z),.5); return g; }

// 10 HALO MKII: the T8 ring grown into a flagship: an armoured ring wing round a broad hull, six SC fans set round the ring
function s10(o){ const g=new T.Group(), K=kit(g), {box,light}=K, on=o.on!==false, A=on?M.amber:M.amberDim, cy=2.5;
  sleekHull(K,7.4,1.9,1.5,cy); const r=ionRing(g,2.0,2.4,on,A,{stages:2,th:.3}); r.position.set(0,cy,0);
  for(let i=0;i<6;i++){ const a=i/6*TAU+TAU/12; if(Math.cos(a)<-.5) continue; const x=Math.sin(a)*2.85, y=cy+Math.cos(a)*1.45; const pz=[1.6,-1.6];
  }
  for(const sd of [-1,1]) for(const z of [2.4,0,-2.4]){ K.tube(V(sd*.9,cy-.2,z),V(sd*2.4,cy-.4,z),.1,M.armour,8); scFan(g,sd*2.75,cy-.75,z,.45,o,L9(on,z>1),z+sd); }
  for(let i=0;i<4;i++){ const a=Math.PI/4+i*Math.PI/2; K.tube(V(Math.sin(a)*.8,cy+Math.cos(a)*.65,0),V(Math.sin(a)*1.97,cy+Math.cos(a)*1.97,0),.07,M.armour,6); }
  if(!o.gearUp) for(const [x,z] of [[.6,2.4],[-.6,2.4],[.6,-2.4],[-.6,-2.4]]) legStrut(K,V(x,cy-.7,z),V(x*1.6,.05,z),.4); light(M.white,0,cy-.3,3.7,.03,g,.4); return g; }

// 11 Y-CARRIER: three armoured arms in a Y round a cargo core, two SC fans in each arm, the bridge on the forward arm
function s11(o){ const g=new T.Group(), K=kit(g), {box,light,plan}=K, on=o.on!==false, A=on?M.amber:M.amberDim;
  for(let i=0;i<3;i++){ const a=i/3*TAU; const ag=new T.Group(); ag.rotation.y=a; g.add(ag); const AK=kit(ag);
    slab(AK,[[-.85,.9],[.85,.9],[.75,3.4],[-.75,3.4]],[[0,1.75,.52],[0,2.9,.4]],1.0,.8,M.armour); scFan(ag,0,.98,1.75,.5,o,L9(on,i===0),i); scFan(ag,0,.98,2.9,.38,o,L9(on,i===0),i+3);
    AK.box(.012,.016,2.5,A,.86,1.4,2.15).castShadow=false; AK.box(.012,.016,2.5,A,-.86,1.4,2.15).castShadow=false;
    if(!o.gearUp) legStrut(AK,V(0,1.0,3.3),V(0,.05,3.45),.45); }
  plan(hexPts(1.35,TAU/12),2.4,M.armour,.5); plan(hexPts(1.1,TAU/12),.3,M.shell2,2.9); plan(hexPts(1.37,TAU/12),.02,A,1.8);
  const br=new T.Group(); br.position.set(0,1.8,2.9); g.add(br); kit(br).prof([[0,0],[.5,0],[.38,.3],[0,.32]],1.1,M.armour); kit(br).prof([[.38,.07],[.49,.03],[.38,.25],[.31,.27]],.9,M.lens);
  light(M.white,0,3.25,0,.03,g,.4); return g; }

// 12 DOUBLE DECK: a two-level ship: the upper deck carries the six SC fans and the bridge, the lower deck is the hold
function s12(o){ const g=new T.Group(), K=kit(g), {box,light}=K, on=o.on!==false, A=on?M.amber:M.amberDim;
  loft(K,[[-3.6,3.0,1.6,.4],[2.6,3.0,1.6,.4],[3.5,2.2,1.2,.35,-.15],[3.8,1.2,.6,.2,-.3]],1.4);
  const OUT=[[2.6,3.4],[3.4,2.6],[3.4,-3.0],[2.8,-3.6],[-2.8,-3.6],[-3.4,-3.0],[-3.4,2.6],[-2.6,3.4]]; const FAN=[[2.3,1.8],[-2.3,1.8],[2.3,-.2],[-2.3,-.2],[2.3,-2.2],[-2.3,-2.2]];
  slab(K,OUT,FAN.map(f=>[f[0],f[1],.6]),2.25,.6,M.armour); FAN.forEach((f,i)=>scFan(g,f[0],2.2,f[1],.55,o,L9(on,f[1]>1),i));
  for(const sd of [-1,1]) box(.012,.016,6.4,A,sd*3.405,2.55,-.2).castShadow=false; box(3.0,.012,.012,A,0,1.0,3.01).castShadow=false;
  const br=new T.Group(); br.position.set(0,2.85,2.0); g.add(br); kit(br).prof([[0,0],[.9,0],[.7,.45],[0,.5]],1.6,M.armour); kit(br).prof([[.7,.1],[.88,.05],[.7,.4],[.6,.42]],1.3,M.lens);
  for(let i=0;i<4;i++) box(.6,.6,.012,M.shell2,-1.0+i*.67,1.4,3.0); light(M.white,0,1.1,3.82,.03,g,.4);
  if(!o.gearUp) for(const [x,z] of [[1.2,2.4],[-1.2,2.4],[1.2,-2.8],[-1.2,-2.8]]) legStrut(K,V(x,.6,z),V(x,.05,z),.5); return g; }

// ===================================================================== T9 MILITARISTIC ROUND: SIX ARMOURED HEAVY LIFTERS (six SC fans, 64 blocks)
const camoTex=ctex(128,128,(x,w,h)=>{ x.fillStyle='#2b2f31'; x.fillRect(0,0,w,h); const cols=['#25292b','#313537','#2a2d2c']; for(let i=0;i<14;i++){ x.fillStyle=cols[i%3]; x.beginPath(); const cx=Math.random()*w, cy=Math.random()*h; x.moveTo(cx,cy); for(let k=0;k<5;k++) x.lineTo(cx+(Math.random()-.5)*60,cy+(Math.random()-.5)*60); x.fill(); }
  x.strokeStyle='#1d2022'; x.lineWidth=2; for(let i=0;i<w;i+=32){ x.beginPath(); x.moveTo(i,0); x.lineTo(i,h); x.stroke(); x.beginPath(); x.moveTo(0,i); x.lineTo(w,i); x.stroke(); }
  x.fillStyle='#4a4f52'; for(let i=0;i<w;i+=32) for(let j=0;j<h;j+=8){ x.fillRect(i+2,j+2,2,2); } });
// world-space triplanar mapping so the plate texture keeps its size on every face and never stretches
function triplanar(mat,scale){ mat.onBeforeCompile=sh=>{ sh.uniforms.uTS={value:scale};
  sh.vertexShader='varying vec3 vTW; varying vec3 vTN;\n'+sh.vertexShader.replace('#include <begin_vertex>','#include <begin_vertex>\nvTW=(modelMatrix*vec4(transformed,1.)).xyz; vTN=normalize(mat3(modelMatrix)*objectNormal);');
  sh.fragmentShader='uniform float uTS; varying vec3 vTW; varying vec3 vTN;\n'+sh.fragmentShader.replace('#include <map_fragment>','vec3 an=abs(vTN); vec2 tuv = an.y>max(an.x,an.z) ? vTW.xz : (an.x>an.z ? vec2(vTW.z,vTW.y) : vTW.xy); tuv*=uTS;\n#ifdef USE_MAP\nvec4 texelColor=texture2D(map,tuv); texelColor=mapTexelToLinear(texelColor); diffuseColor*=texelColor;\n#endif'); };
  mat.customProgramCacheKey=()=>'tri'+scale; return mat; }
const plateTex=ctex(128,128,(x,w,h)=>{ x.fillStyle='#2e3234'; x.fillRect(0,0,w,h); const im=x.getImageData(0,0,w,h),d=im.data; for(let i=0;i<d.length;i+=4){ const v=(Math.random()-.5)*6; d[i]+=v; d[i+1]+=v; d[i+2]+=v; } x.putImageData(im,0,0);
  x.fillStyle='#34393b'; x.fillRect(0,0,64,64); x.fillRect(64,64,64,64); x.fillStyle='#1c1f21'; for(const q of [0,64]){ x.fillRect(q,0,2,h); x.fillRect(0,q,w,2); } x.fillStyle='rgba(255,255,255,.07)'; for(const q of [2,66]){ x.fillRect(q,0,1,h); x.fillRect(0,q,w,1); }
  x.fillStyle='#585e62'; for(const q of [6,58,70,122]) for(const r of [6,58,70,122]) x.fillRect(q,r,2,2); });
plateTex.wrapS=plateTex.wrapT=T.RepeatWrapping; plateTex.magFilter=T.LinearFilter;
M.mil=triplanar(std({map:shellTex, color:0xe2e5e8, roughness:.55, metalness:.35, flatShading:true}),2.0); M.milD=triplanar(std({map:shellTex, color:0xe2e5e8, roughness:.55, metalness:.35, flatShading:true, side:T.DoubleSide}),2.0);
// armour plate with a bolt row; chaff dispenser block; sensor turret; unit stencil
function plate(K,w,h,d,x,y,z,rx=0,ry=0,rz=0){ const p=cbox(K,w,h,d,Math.min(w,h)*.12,M.mil,x,y,z,rx,ry,rz); return p; }
function dispenser(K,x,y,z,ry){ const g=new T.Group(); g.position.set(x,y,z); g.rotation.y=ry; K.add(new T.BoxGeometry(.001,.001,.001),M.black,0,0,0); return g; }
function chaff(g,x,y,z,ry){ const d=new T.Group(); d.position.set(x,y,z); d.rotation.y=ry; g.add(d); const K=kit(d); K.box(.36,.22,.12,M.shell2,0,0,0); for(let i=0;i<4;i++) for(let j=0;j<2;j++) K.cyl(.03,.02,8,M.black,-.12+i*.08,-.05+j*.1,.061,Math.PI/2); }
function turret(g,x,y,z){ const t=new T.Group(); t.position.set(x,y,z); g.add(t); const K=kit(t); K.cyl(.16,.08,10,M.shell2,0,0,0); K.add(new T.SphereGeometry(.15,12,10),M.mil,0,-.12,0); K.box(.14,.08,.04,M.lens,0,-.12,.13); K.cyl(.03,.02,8,M.lens,.05,-.06,.15,Math.PI/2); }
function unit(g,txt,x,y,z,ry,w=.6){ const st=new T.Mesh(new T.PlaneGeometry(w,w*.28),stencil(txt,'#c9cdd1',256,72,'700 52px "DejaVu Sans Mono", monospace')); st.position.set(x,y,z); st.rotation.y=ry; g.add(st); }
function chevrons(K,x,y,z,w,ry=0){ const p=K.box(w,.004,.18,M.haz,x,y,z); p.rotation.y=ry; return p; }
function milFan(g,x,y,z,R,o,lit,k){ const f=scFan(g,x,y,z,R,o,lit,k); const K=kit(f); const H=R*.75; for(let i=0;i<8;i++){ const a=i/8*TAU+Math.PI/8; K.box(.06,H+.04,.24,M.mil,Math.sin(a)*(R+.19),H/2,Math.cos(a)*(R+.19),0,a,0); } return f; }

// M1 ARMOURED DROPSHIP: a faceted armoured fuselage, four SC fans in armoured pods on stub wings, two in the tail, rear ramp
function m1(o){ const g=new T.Group(), K=kit(g), {box,light}=K, on=o.on!==false, A=on?M.amber:M.amberDim;
  loft(K,[[-3.4,1.7,1.6,.45],[1.6,1.9,1.8,.55],[2.7,1.5,1.3,.45,-.2],[3.5,.7,.6,.22,-.4],[3.8,.2,.2,.06,-.45]],1.85,M.mil);
  loft(K,[[1.9,1.3,.4,.12,.62],[2.9,.8,.25,.08,.25]],1.85,M.lens); turret(g,0,1.15,2.6);
  for(const sd of [-1,1]){ plate(K,1.7,.16,1.2,sd*1.65,2.1,.6,0,0,-sd*.12); for(const z of [1.4,-.2]){ cbox(K,1.5,.5,1.5,.25,M.mil,sd*2.55,1.85,z); milFan(g,sd*2.55,1.6,z,.5,o,L9(on,z>0),z+sd); }
    chaff(g,sd*.96,2.3,-1.2,sd*Math.PI/2); unit(g,'09',sd*.97,1.85,.3,sd*Math.PI/2,.5); box(.012,.016,4.8,A,sd*.96,1.35,-.2).castShadow=false; light(sd>0?M.white:A,sd*3.3,1.85,.6,.022,g,.32); }
  for(const sd of [-1,1]) { cbox(K,.9,.9,1.6,.2,M.mil,sd*1.1,2.2,-2.6); milFan(g,sd*1.1,2.32,-2.75,.42,o,A,sd*7); }
  for(let i=0;i<5;i++) box(1.0,.006,.006,M.black,0,2.76,-1.6+i*.6);
  box(1.5,.01,.01,M.black,0,1.08,-3.38); chevrons(K,0,1.06,-3.0,1.2);
  if(!o.gearUp) for(const [x,z] of [[.8,1.6],[-.8,1.6],[.8,-2.2],[-.8,-2.2]]) legStrut(K,V(x,1.2,z),V(x*1.2,.05,z),.5);
  light(A,0,2.8,-3.4,.022,g,.32); return g; }

// M2 ARMOURED HAULER: a long angular armoured hull, three SC fans in armoured sponsons down each side, sensor block forward
function m2(o){ const g=new T.Group(), K=kit(g), {box,light}=K, on=o.on!==false, A=on?M.amber:M.amberDim;
  loft(K,[[-3.7,2.0,1.6,.5,.1],[2.2,2.2,1.9,.6],[3.2,1.6,1.4,.5,-.2],[3.75,.8,.6,.25,-.45]],1.95,M.mil);
  const sb=new T.Group(); sb.position.set(0,2.7,2.2); g.add(sb); kit(sb).prof([[0,0],[.9,0],[.7,.45],[0,.5]],1.4,M.mil); kit(sb).prof([[.7,.12],[.88,.06],[.7,.38],[.6,.42]],1.0,M.lens); turret(g,0,1.12,3.0);
  for(const sd of [-1,1]){ for(const z of [2.0,0,-2.0]){ cbox(K,1.2,.6,1.5,.25,M.mil,sd*2.3,1.75,z); milFan(g,sd*2.4,1.5,z,.48,o,L9(on,z>1),z+sd); }
    for(let i=0;i<6;i++) plate(K,.08,1.2,.9,sd*1.13,1.95,-2.7+i*1.0,0,0,sd*.1); chaff(g,sd*1.15,2.75,-2.8,sd*Math.PI/2); unit(g,'09',sd*1.16,2.2,1.4,sd*Math.PI/2,.55); }
  box(1.6,.01,.01,M.black,0,1.16,-3.68); chevrons(K,0,1.16,-3.3,1.4);
  if(!o.gearUp) for(const [x,z] of [[.9,2.0],[-.9,2.0],[.9,-2.6],[-.9,-2.6]]) legStrut(K,V(x,1.2,z),V(x*1.1,.05,z),.5);
  light(M.white,0,1.8,3.78,.025,g,.35); light(A,0,2.8,-3.7,.022,g,.32); return g; }

// M3 HOVER FORTRESS: a low armoured hull like a hover tank: angled glacis plates, six SC fans under armoured grilles, side skirts
function m3(o){ const g=new T.Group(), K=kit(g), {box,light}=K, on=o.on!==false, A=on?M.amber:M.amberDim;
  const OUT=[[2.2,3.7],[3.1,2.6],[3.2,-3.2],[2.6,-3.7],[-2.6,-3.7],[-3.2,-3.2],[-3.1,2.6],[-2.2,3.7]]; const FAN=[[2.15,1.8],[-2.15,1.8],[2.15,-.2],[-2.15,-.2],[2.15,-2.25],[-2.15,-2.25]];
  slab(K,OUT,FAN.map(f=>[f[0],f[1],.72]),.75,1.1,M.mil); FAN.forEach((f,i)=>{ milFan(g,f[0],.95,f[1],.5,o,L9(on,f[1]>1),i); for(let k=-3;k<=3;k++) box(.05,.05,2*Math.sqrt(Math.max(0,.72*.72-(k*.2)**2)),M.shell2,f[0]+k*.2,1.88,f[1]); });
  loft(K,[[-3.2,2.6,.9,.35],[1.2,2.6,.9,.35],[2.4,2.0,.6,.25,-.15]],2.3,M.mil); plate(K,2.4,.12,1.2,0,2.3,2.9,-.45,0,0);
  const br=new T.Group(); br.position.set(0,2.75,1.0); g.add(br); kit(br).prof([[0,0],[.8,0],[.55,.4],[0,.45]],1.2,M.mil); kit(br).prof([[.55,.1],[.78,.04],[.55,.34],[.45,.38]],.9,M.lens);
  for(const sd of [-1,1]){ for(let i=0;i<5;i++) plate(K,.1,.7,1.3,sd*3.25,1.1,-2.8+i*1.4,0,0,sd*.18); chaff(g,sd*1.32,2.7,-1.8,sd*Math.PI/2); unit(g,'09',sd*3.33,1.25,2.2,sd*Math.PI/2,.6); box(.012,.016,6.4,A,sd*3.21,1.8,-.2).castShadow=false; }
  turret(g,0,.72,3.2);
  if(!o.gearUp) for(const [x,z] of [[1.0,2.6],[-1.0,2.6],[1.0,-2.9],[-1.0,-2.9]]) legStrut(K,V(x,.75,z),V(x,.05,z),.55);
  light(M.white,0,1.3,3.72,.025,g,.35); return g; }

// M4 ARMOURED WING: a thick faceted flying wing like a heavy bomber, six SC fans under armoured doors, armoured cargo bay
function m4(o){ const g=new T.Group(), K=kit(g), {box,light}=K, on=o.on!==false, A=on?M.amber:M.amberDim;
  const OUT=[[0,3.7],[1.6,2.0],[3.4,-.8],[3.4,-1.6],[2.4,-1.2],[1.6,-2.4],[.8,-1.9],[0,-3.0],[-.8,-1.9],[-1.6,-2.4],[-2.4,-1.2],[-3.4,-1.6],[-3.4,-.8],[-1.6,2.0]];
  const FAN=[[1.05,.9],[-1.05,.9],[2.25,-.55],[-2.25,-.55],[.95,-1.0],[-.95,-1.0]];
  slab(K,OUT,FAN.map(f=>[f[0],f[1],.6]),1.0,.75,M.mil); slab(K,scl(OUT,.85),FAN.map(f=>[f[0],f[1],.66]),1.75,.18,M.mil);
  FAN.forEach((f,i)=>{ milFan(g,f[0],1.05,f[1],.46,o,L9(on,f[1]>0),i); for(const sd of [-1,1]){ const dp=new T.Group(); dp.position.set(f[0]+sd*.62,1.93,f[1]); dp.rotation.z=sd*1.1; g.add(dp); kit(dp).box(.6,.04,1.2,M.mil,-sd*.3,0,0); } });
  loft(K,[[-2.2,1.2,.6,.2,.1],[2.0,1.1,.55,.18,.1],[3.2,.4,.3,.1,-.05]],2.0,M.mil); loft(K,[[2.2,.8,.2,.06,.32],[3.1,.3,.12,.04,.12]],2.0,M.lens);
  for(const sd of [-1,1]){ edgeStrip(K,[sd*.05,3.65],[sd*1.58,2.0],sd,1.38,A); unit(g,'09',sd*2.5,1.94,-1.0,0,.5); g.children[g.children.length-1].rotation.x=-Math.PI/2; chaff(g,sd*.62,2.25,-1.6,sd*Math.PI/2); }
  turret(g,0,.98,2.6); box(1.4,.01,2.0,M.black,0,.99,0);
  if(!o.gearUp) for(const [x,z] of [[0,2.3],[1.2,-1.5],[-1.2,-1.5]]) legStrut(K,V(x,1.0,z),V(x,.05,z),.5); return g; }

// M5 ARMOURED TWIN-BOOM: a heavy armoured hold, two armoured booms with three SC fans each, a sensor mast, tail plane
function m5(o){ const g=new T.Group(), K=kit(g), {box,light}=K, on=o.on!==false, A=on?M.amber:M.amberDim;
  loft(K,[[-2.6,2.1,1.9,.55],[1.8,2.1,1.9,.55],[2.9,1.5,1.4,.45,-.15],[3.6,.7,.6,.22,-.4]],1.75,M.mil); loft(K,[[2.0,1.4,.42,.12,.66],[2.9,.8,.25,.08,.3]],1.75,M.lens); turret(g,0,.8,2.9);
  for(const sd of [-1,1]){ const bx=sd*2.4; const b=loft(K,[[-3.7,.6,.6,.18],[3.0,.8,.8,.24],[3.6,.4,.4,.12]],2.35,M.mil); b.position.x=bx;
    for(const z of [2.1,0,-2.1]){ cbox(K,1.25,.4,1.25,.22,M.mil,bx,2.15,z); milFan(g,bx,1.85,z,.46,o,L9(on,z>1),z+sd); }
    K.tube(V(sd*1.05,2.2,.8),V(bx,2.2,.8),.12,M.mil,8); K.tube(V(sd*1.05,2.2,-1.2),V(bx,2.2,-1.2),.12,M.mil,8); chaff(g,sd*1.06,2.5,-1.8,sd*Math.PI/2); unit(g,'09',sd*1.06,1.7,.4,sd*Math.PI/2,.55);
    fin(K,[[-3.7,0],[-3.0,0],[-3.4,.8],[-3.75,.85]],bx,2.6,0,0,.06); light(sd>0?M.white:A,bx,2.35,3.62,.02,g,.3); }
  cbox(K,4.8,.1,.5,.03,M.mil,0,3.3,-3.5); box(.03,.6,.03,M.anod,0,3.0,-.8); for(const y of [3.05,3.2]) box(.4,.02,.02,M.anod,0,y,-.8);
  box(1.8,.01,.01,M.black,0,.85,-2.58); chevrons(K,0,.84,-2.2,1.6);
  if(!o.gearUp) for(const [x,z] of [[.9,1.6],[-.9,1.6],[.9,-2.0],[-.9,-2.0]]) legStrut(K,V(x,.85,z),V(x*1.1,.05,z),.5); return g; }

// M6 ARMOURED SKY CRANE: an armoured spine and bridge, three SC fans on armoured pylons each side, the 64-block module pack clamped below
function m6(o){ const g=new T.Group(), K=kit(g), {box,light}=K, on=o.on!==false, A=on?M.amber:M.amberDim;
  loft(K,[[-3.6,1.2,1.0,.3,.1],[1.6,1.3,1.1,.32],[2.8,1.6,1.4,.4,.1],[3.6,.8,.7,.22,-.1]],3.55,M.mil); loft(K,[[2.9,1.2,.4,.12,.5],[3.5,.6,.25,.08,.2]],3.55,M.lens); turret(g,0,2.75,3.2);
  for(const sd of [-1,1]){ for(const z of [2.0,0,-2.0]){ K.tube(V(sd*.6,3.5,z),V(sd*1.9,3.4,z),.13,M.mil,8); cbox(K,1.2,.4,1.2,.22,M.mil,sd*2.45,3.35,z); milFan(g,sd*2.45,3.05,z,.48,o,L9(on,z>1),z+sd); }
    chaff(g,sd*.62,3.9,-2.6,sd*Math.PI/2); unit(g,'09',sd*.62,3.55,-1.0,sd*Math.PI/2,.5);
    for(const z of [-2.2,-.75,.75,2.2]){ K.tube(V(sd*.4,3.0,z),V(sd*1.1,2.4,z),.05,M.steel,6); box(.08,.3,.3,M.haz,sd*1.13,2.3,z); } }
  if(o.cargo) modules(K,2,4,1.75,0,1.0,1.4,.9), modules(K,2,4,.8,0,1.0,1.4,.9);
  box(.012,.016,6.0,A,0,4.13,-.6).castShadow=false;
  if(!o.gearUp) for(const [x,z] of [[1.3,2.9],[-1.3,2.9],[1.3,-2.9],[-1.3,-2.9]]) legStrut(K,V(x,3.1,z),V(x*1.2,.05,z),.5);
  light(M.white,0,3.4,3.62,.025,g,.35); return g; }

// ===================================================================== T9 ARMOURED DROPSHIP (Justin picked militaristic option 1)
// Faceted armoured fuselage in dark disruptive pattern with bolted plate rows; glass sensor canopy and chin sensor turret;
// stub wings carrying four armoured SC fan pods that gimbal; two more SC fans in armoured tail nacelles; dorsal spine
// with aerials and a satcom blister; countermeasure dispensers; unit 09 and hazard markings; rear ramp opening onto a
// hold of eight 8-block modules (64 blocks); folding main legs under the wing pods and a twin-wheel nose leg.
function t9(o){
  const g=new T.Group(), K=kit(g), {box,cyl,tube,light,add}=K, on=o.on!==false, A=on?M.amber:M.amberDim, up=!!o.gearUp, tilt=(o.cruise||0)*.6;
  const open=o.cargo==='winch';
  loft(K,[[-3.4,1.7,1.6,.45],[1.6,1.9,1.8,.55],[2.7,1.5,1.3,.45,-.2],[3.5,.7,.6,.22,-.4],[3.8,.2,.2,.06,-.45]],1.85,M.milD,[open?0:1,1]);
  if(open){ const fr=loft(K,[[-3.42,1.72,1.62,.46],[-3.36,1.72,1.62,.46]],1.85,M.shell2,[0,0]); }
  loft(K,[[1.9,1.3,.4,.12,.62],[2.9,.8,.25,.08,.25]],1.85,M.lens); turret(g,0,1.15,2.6);
  // armour plate rows and bolts along the sides, belly keel plates
  for(const sd of [-1,1]){ for(let i=0;i<6;i++){ const z=-3.0+i*.85; plate(K,.06,.95,.78,sd*.955,1.85,z,0,0,sd*.04); for(let b=0;b<3;b++) box(.012,.03,.03,M.steel,sd*.99,2.25-b*.4,z-.32); }
    unit(g,'09',sd*.99,1.9,1.1,sd*Math.PI/2,.55); chaff(g,sd*.96,2.42,-1.0,sd*Math.PI/2); chaff(g,sd*.96,2.42,-1.5,sd*Math.PI/2);
    box(.012,.016,4.8,A,sd*.96,1.32,-.4).castShadow=false; for(let k=0;k<6;k++) box(.008,.12,.035,M.black,sd*.97,2.1,2.0+k*.08); }
  for(let i=0;i<4;i++) plate(K,1.2,.08,.8,0,1.07,-2.6+i*.9);
  // dorsal spine, satcom blister, aerials, strobes
  loft(K,[[-3.0,.6,.3,.08],[1.4,.6,.3,.08],[2.0,.3,.15,.05,-.05]],2.8,M.mil); box(.012,.008,4.2,A,0,2.955,-.8).castShadow=false;
  { const d=add(new T.SphereGeometry(.28,14,8,0,TAU,0,Math.PI/2),M.grey,0,2.92,-1.9); d.scale.set(1,.45,1.2); }
  for(const z of [.4,-.6]) tube(V(.2,2.95,z),V(.25,3.35,z-.1),.012,M.anod,6); light(M.white,.25,3.36,-.7,.016); light(A,0,2.96,-3.0,.02,g,.3);
  // stub wings with armoured fan pods (gimbal for cruise); tip lights
  for(const sd of [-1,1]){ plate(K,1.7,.16,1.4,sd*1.65,2.1,.55,0,0,-sd*.12); box(1.6,.012,.012,A,sd*1.65,2.1,1.26).castShadow=false;
    for(const z of [1.4,-.3]){ const pg=new T.Group(); pg.position.set(sd*2.55,1.85,z); pg.rotation.x=tilt; g.add(pg); const PK=kit(pg); cbox(PK,1.5,.5,1.5,.25,M.mil,0,0,0);
      for(let k=0;k<3;k++) PK.box(1.2,.006,.006,M.black,0,.255,-.45+k*.45); milFan(pg,0,-.25,0,.5,o,L9(on,z>0),z+sd); PK.box(1.52,.012,.012,A,0,-.1,.76).castShadow=false; }
    light(sd>0?M.white:A,sd*3.32,1.95,.55,.022,g,.32); }
  // tail nacelles with two more SC fans, canted fins
  for(const sd of [-1,1]){ cbox(K,.95,.95,1.7,.22,M.mil,sd*1.15,2.2,-2.6); milFan(g,sd*1.15,2.3,-2.75,.42,o,A,sd*7);
    fin(K,[[-3.5,0],[-2.6,0],[-3.1,.9],[-3.55,.95]],sd*1.15,2.65,0,-sd*.35,.06); light(A,sd*1.5,3.45,-3.45,.018,g,.26); }
  // rear ramp and hold: closed shows the seam and chevrons; open drops the ramp onto the hold of eight modules
  if(open){ const hb=box(1.5,1.3,.05,M.black,0,1.85,1.0); hb.castShadow=false; light(M.amber,0,2.45,-.5,.02,g,.3); light(M.amber,0,2.45,-2.4,.02,g,.3); modules(K,2,4,1.6,-1.25,.62,.95,.62,.08); modules(K,2,4,2.3,-1.25,.62,.95,.62,.08);
    const rp=new T.Group(); rp.position.set(0,1.08,-3.36); rp.rotation.x=-.75; g.add(rp); const RK=kit(rp); RK.box(1.5,.06,1.5,M.mil,0,0,-.75); for(let k=0;k<5;k++) RK.box(1.3,.01,.05,M.amberPaint,0,.035,-.2-k*.28);
    light(M.white,0,2.5,-3.0,.02,g,.3); }
  else { box(1.5,.01,.01,M.black,0,1.08,-3.38); for(const sd of [-1,1]) box(.01,1.25,.01,M.black,sd*.75,1.7,-3.32); chevrons(K,0,1.06,-3.0,1.2); }
  // landing gear: folding main legs under the wing pods, twin-wheel nose leg
  for(const sd of [-1,1]) mainLeg(g,sd*2.55,1.5,.55,sd,up);
  wheelLeg(g,0,1.12,2.2,up);
  light(M.white,0,1.62,3.55,.026,g,.35);
  return g; }

// ===================================================================== T9 ARMOURED DROPSHIP MK2 (shape rework)
// Chined, faceted hull (six-sided sections: flat belly, sharp chines, sloped upper faces) with the canopy glass laid into
// the upper nose facets; blended wings with four superconducting fans recessed in them (cryocooler domes flush under
// small hatches, coil band glowing inside the lip); a blended tail with two more recessed fans and twin canted fins;
// flush countermeasure ports; chin sensor turret; rear ramp onto eight 8-block modules; folding legs, nose wheel.
function ringLoft(K,rings,mat,caps=[1,1]){ const pos=[]; const tri=(p,q,r)=>pos.push(p.x,p.y,p.z,q.x,q.y,q.z,r.x,r.y,r.z); const n=rings[0].length;
  for(let i=0;i<rings.length-1;i++) for(let k=0;k<n;k++){ const a=rings[i][k], b=rings[i][(k+1)%n], c=rings[i+1][(k+1)%n], d=rings[i+1][k]; tri(a,b,c); tri(a,c,d); }
  for(const [R,flip,on] of [[rings[0],1,caps[0]],[rings[rings.length-1],0,caps[1]]]){ if(!on) continue; const cen=R.reduce((m,p)=>m.add(p),V(0,0,0)).multiplyScalar(1/n); for(let k=0;k<n;k++){ const a=R[k], b=R[(k+1)%n]; flip?tri(cen,b,a):tri(cen,a,b); } }
  const ge=new T.BufferGeometry(); ge.setAttribute('position',new T.Float32BufferAttribute(pos,3)); ge.computeVertexNormals(); const uv=[]; for(let i=0;i<pos.length;i+=3) uv.push(0,0); ge.setAttribute('uv',new T.Float32BufferAttribute(uv,2)); return K.add(ge,mat,0,0,0); }
const hexSec=(z,wt,wc,wb,yt,yc,yb)=>[V(-wb/2,yb,z),V(wb/2,yb,z),V(wc/2,yc,z),V(wt/2,yt,z),V(-wt/2,yt,z),V(-wc/2,yc,z)];
function quadPanel(K,a,b,c,d,mat,off=.012){ const n=b.clone().sub(a).cross(d.clone().sub(a)).normalize().multiplyScalar(off); const P=[a,b,c,d].map(p=>p.clone().add(n)); const pos=[]; for(const [i,j,k] of [[0,1,2],[0,2,3]]) pos.push(...P[i].toArray(),...P[j].toArray(),...P[k].toArray());
  const ge=new T.BufferGeometry(); ge.setAttribute('position',new T.Float32BufferAttribute(pos,3)); ge.computeVertexNormals(); const m=K.add(ge,mat,0,0,0); m.material=mat; return m; }
function recessFan(g,x,y,z,R,H,o,lit,k){ const fg=fanHole(g,x,y,z,R,H,lit,o,k); const K=kit(fg); const cb=new T.Mesh(new T.TorusGeometry(R-.01,.018,6,48),lit); cb.rotation.x=Math.PI/2; cb.position.y=H*.82; fg.add(cb);
  for(let i=0;i<3;i++){ const a=i/3*TAU+k; const hx=Math.sin(a)*(R+.2), hz=Math.cos(a)*(R+.2); K.box(.22,.012,.22,M.shell2,hx,H+.006,hz); K.add(new T.SphereGeometry(.06,10,6,0,TAU,0,Math.PI/2),M.steel,hx,H+.01,hz); } return fg; }
function t9b(o){
  const g=new T.Group(), K=kit(g), {box,cyl,tube,light,add}=K, on=o.on!==false, A=on?M.amber:M.amberDim, up=!!o.gearUp, open=o.cargo==='winch';
  const S=[[3.9,.04,.08,.04,1.72,1.70,1.66],[3.1,.34,1.0,.46,2.02,1.76,1.42],[1.9,.72,1.86,1.08,2.52,1.86,1.12],[.2,.9,2.12,1.32,2.74,1.9,1.04],[-2.3,.9,2.02,1.32,2.70,1.9,1.04],[-3.45,.72,1.66,1.12,2.42,1.86,1.08]];
  const rings=S.map(q=>hexSec(...q)); ringLoft(K,rings,M.milD,[1,open?0:1]);
  const seam=(R)=>{ for(let k=0;k<R.length;k++){ const p0=R[k], p1=R[(k+1)%R.length]; const c=V(0,(p0.y+p1.y)/2,p0.z); const out=p0.clone().add(p1).multiplyScalar(.5).sub(c).normalize().multiplyScalar(.008); tube(p0.clone().add(out),p1.clone().add(out),.009,M.black,4).castShadow=false; } };
  for(const [i,f] of [[2,.0],[2,.55],[3,.35],[3,.75],[4,.5]]) seam(rings[i].map((p,k)=>p.clone().lerp(rings[i+1][k],f)));
  // canopy glass laid into the upper facets between the nose stations
  const lerpR=(i,f)=>rings[i].map((p,k)=>p.clone().lerp(rings[i+1][k],f));
  const r0=lerpR(1,.15), r1=lerpR(1,.95); for(const [a,b] of [[3,4],[2,3],[4,5]]) quadPanel(K,r1[a],r1[b],r0[b],r0[a],M.lens,.014);
  // chine light lines, flush countermeasure ports, unit marks, aerials, dorsal sensor fairing, strobe
  for(const sd of [-1,1]){ const p0=rings[2][sd>0?2:5], p1=rings[4][sd>0?2:5]; const m=box(.014,.014,p0.distanceTo(p1),A,p0.x+sd*.005,p0.y,(p0.z+p1.z)/2); m.castShadow=false;
    for(let i=0;i<2;i++) for(let j=0;j<4;j++) box(.01,.05,.05,M.black,sd*1.04,2.2+i*.08,-1.6+j*.08);
    unit(g,'09',sd*1.02,1.55,.6,sd*Math.PI/2,.5); }
  loft(K,[[-1.6,.5,.18,.06],[.6,.5,.18,.06],[1.0,.3,.12,.04]],2.83,M.milD); tube(V(.2,2.9,-1.2),V(.24,3.25,-1.3),.012,M.anod,6); light(M.white,.24,3.26,-1.3,.016); light(A,0,2.92,.9,.016);
  turret(g,0,1.12,2.75);
  // blended wings with four recessed SC fans
  const WING=[[.95,1.55],[2.6,.95],[3.35,.55],[3.35,-.95],[2.9,-1.45],[.95,-1.65]];
  const FW=[[2.1,.45],[2.35,-.85]];
  for(const sd of [-1,1]){ const W=WING.map(q=>[q[0]*sd,q[1]]); const holes=FW.map(f=>[f[0]*sd,f[1],.5]);
    slab(K,W,holes,1.62,.36,M.milD); slab(K,W.map(q=>[q[0]*.985,q[1]*.97]).map((q,i)=>i===0||i===5?[sd*1.0,q[1]]:q),FW.map(f=>[f[0]*sd,f[1],.56]),1.98,.06,M.milD);
    FW.forEach((f,i)=>recessFan(g,f[0]*sd,1.58,f[1],.5,.44,o,L9(on,f[1]>0),i+sd));
    edgeStrip(K,[sd*.96,1.53],[sd*2.58,.95],sd>0?-1:1,1.80,A); light(sd>0?M.white:A,sd*3.37,1.82,-.2,.022,g,.32); box(.03,.3,1.4,M.milD,sd*3.36,1.8,-.2);
    unit(g,'09',sd*2.9,2.05,-.2,0,.5); g.children[g.children.length-1].rotation.x=-Math.PI/2; }
  // blended tail with two recessed fans and twin canted fins
  const TAIL=[[.9,-2.2],[2.15,-2.6],[2.15,-3.45],[.9,-3.45]];
  for(const sd of [-1,1]){ slab(K,TAIL.map(q=>[q[0]*sd,q[1]]),[[sd*1.5,-2.98,.38]],1.72,.42,M.milD); recessFan(g,sd*1.5,1.68,-2.98,.38,.46,o,A,sd*5);
    fin(K,[[-3.45,0],[-2.45,0],[-3.05,1.0],[-3.5,1.05]],sd*2.05,2.12,0,-sd*.4,.07,M.milD); light(A,sd*2.47,3.05,-3.42,.018,g,.26); }
  // ramp and hold
  if(open){ const hb=box(1.5,1.3,.05,M.black,0,1.85,1.0); hb.castShadow=false; light(M.amber,0,2.45,-.5,.02,g,.3); light(M.amber,0,2.45,-2.4,.02,g,.3);
    modules(K,2,4,1.40,-1.0,.48,.9,.5,.06); modules(K,2,4,1.94,-1.0,.48,.9,.5,.06);
    const rp=new T.Group(); rp.position.set(0,1.08,-3.45); rp.rotation.x=-.75; g.add(rp); const RK=kit(rp); RK.box(1.3,.06,1.5,M.milD,0,0,-.75); for(let k=0;k<5;k++) RK.box(1.1,.01,.05,M.amberPaint,0,.035,-.2-k*.28); }
  else { const r=rings[5]; for(const [a,b] of [[0,1],[1,2],[5,0]]) { const m=quadPanel(K,r[a],r[b],r[b],r[a],M.black,.004); } chevrons(K,0,1.07,-3.1,1.0); }
  for(const sd of [-1,1]) mainLeg(g,sd*2.95,1.62,-.2,sd,up);
  wheelLeg(g,0,1.4,2.35,up);
  light(M.white,0,1.71,3.9,.024,g,.34);
  return g; }

// ===================================================================== FINISH PASS (Justin: every drone made the T9 way)
// No painted textures that stretch: all skins are mapped in world space at a fixed size (triplanar), cargo boxes use a
// fixed-size corrugation with geometric amber bands and correctly proportioned label decals, seams are real geometry.
for(const [k,sc] of [['shell',2.0],['armour',2.0],['armourD',2.0],['duct',2.0],['carbon',6.0],['env',1.0],['envD',1.0]]) if(M[k]) triplanar(M[k],sc);
const ribTex=ctex(64,64,(x,w,h)=>{ x.fillStyle='#454c52'; x.fillRect(0,0,w,h); for(let i=0;i<w;i+=8){ x.fillStyle='#363c41'; x.fillRect(i,0,3,h); x.fillStyle='#525a60'; x.fillRect(i+3,0,1,h); } });
ribTex.magFilter=T.LinearFilter;
M.crate=triplanar(std({map:ribTex, roughness:.6, metalness:.3}),4.0); M.mod=M.crate; M.mod8=M.crate; M.mod2=M.crate;
function bandLabels(K,w,h,d,x,y,z,txt){ const bh=Math.min(.2,h*.22); K.box(w+.008,bh,d+.008,M.amberPaint,x,y,z);
  if(!txt) return; for(const [fw,px,pz,ry] of [[w,0,d/2+.006,0],[w,0,-d/2-.006,Math.PI],[d,w/2+.006,0,Math.PI/2],[d,-w/2-.006,0,-Math.PI/2]]){ const lw=Math.min(fw*.8,bh*4.6), lh=lw/4.6;
    const m=new T.Mesh(new T.PlaneGeometry(lw,lh),stencil(txt,'#16181a',230,50,'700 34px "DejaVu Sans Mono", monospace')); m.position.set(x+px,y,z+pz); m.rotation.y=ry; K.add(new T.BoxGeometry(.001,.001,.001),M.black,0,0,0).parent.add(m); } }
function crateBox(K,w,h,d,x,y,z){ const c=K.box(w,h,d,M.crate,x,y,z); bandLabels(K,w,h,d,x,y,z,'SEALED'); for(const sx of [-1,1]) for(const sz of [-1,1]) K.box(.03,h+.004,.03,M.shell2,x+sx*(w/2-.01),y,z+sz*(d/2-.01)); return c; }
cargoModule=function(K,w,h,d,x,y,z){ K.box(w,h,d,M.mod,x,y,z); bandLabels(K,w,h,d,x,y+h*.12,z,'CARGO 16'); for(const sx of [-1,1]) for(const sy of [-1,1]) for(const sz of [-1,1]) K.box(.10,.10,.10,M.anod,x+sx*(w/2-.04),y+sy*(h/2-.04),z+sz*(d/2-.04)); };
cargoModule8=function(K,w,h,d,x,y,z){ K.box(w,h,d,M.mod,x,y,z); bandLabels(K,w,h,d,x,y+h*.12,z,'CARGO 8'); for(const sx of [-1,1]) for(const sy of [-1,1]) for(const sz of [-1,1]) K.box(.08,.08,.08,M.anod,x+sx*(w/2-.03),y+sy*(h/2-.03),z+sz*(d/2-.03)); };

const BUILD={1:t1,2:t2,3:t3,4:t4,5:t5,6:t6,7:t7,8:t8,9:t9b,'9old':t9,'m1':m1,'m2':m2,'m3':m3,'m4':m4,'m5':m5,'m6':m6,'s1':s1,'s2':s2,'s3':s3,'s4':s4,'s5':s5,'s6':s6,'s7':s7,'s8':s8,'s9':s9,'s10':s10,'s11':s11,'s12':s12,'v1':v1,'v2':v2,'v3':v3,'v4':v4,'v5':v5,'v6':v6,'r1':r1,'r2':r2,'r3':r3,'r4':r4,'r5':r5,'r6':r6,'r7':r7,'r8':r8,'r9':r9,'r10':r10,'r11':r11,'r12':r12,'q1':q1,'q2':q2,'q3':q3,'q4':q4,'q5':q5,'q6':q6,'q7':q7,'q8':q8,'q9':q9,'q10':q10,'q11':q11,'q12':q12,'p1':o7_1,'p2':o7_2,'p3':o7_3,'p4':o7_4,'p5':o7_5,'p6':o7_6,'p7':o7_7,'p8':o7_8,'p9':o7_9,'p10':o7_10,'p11':o7_11,'p12':o7_12,'6a':t6a,'6b':t6b,'6c':t6c,'o1':o6_1,'o2':o6_2,'o3':o6_3,'o4':o6_4,'o5':o6_5,'o6':o6_6,'o7':o6_7,'o8':o6_8,'o9':o6_9,'o10':o6_10,'o11':o6_11,'o12':o6_12,'5a':t5a,'5b':t5b,'5c':t5c,'4a':t4a,'4b':t4b,'4c':t4c};
return { build:(tier,o={})=>BUILD[tier](o), M };
};
