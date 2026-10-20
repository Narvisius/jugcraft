"""Recover owner model geometry as data; never load supplied bytecode.

Input: CFR output in ../creature-recovery-oct19/decompiled. Numeric expressions
use a restricted AST interpreter, not eval. Unsupported geometry is reported
and never substituted with a guessed mesh.
"""
import ast
import hashlib
import json
import math
import re
from pathlib import Path
from recover_wildlife_models import split_args, calls, write

ROOT = Path(__file__).resolve().parents[1]
INPUT = ROOT.parent / 'creature-recovery-oct19/decompiled'
OUT = ROOT / 'src/main/resources/assets/jugcraft/recovered_creatures'
REPORT = ROOT / 'art/owner-library/catalog/creature-recovery-2026-10-19'
RENAMES = {'m_171576_':'getRoot','m_171599_':'addOrReplaceChild','m_171558_':'create',
           'm_171514_':'texOffs','m_171488_':'addBox','m_171419_':'offset',
           'm_171423_':'offsetAndRotation','m_171565_':'create','m_171480_':'mirror',
           'm_171555_':'mirror','m_171324_':'getChild',
           'func_78789_a':'addBox','func_78790_a':'addBox','func_78793_a':'setPos',
           'func_78792_a':'addChild','field_78090_t':'texWidth','field_78089_u':'texHeight',
           'field_78809_i':'mirror','field_78795_f':'rotateAngleX','field_78796_g':'rotateAngleY',
           'field_78808_h':'rotateAngleZ',
           'class_5607':'LayerDefinition','class_5609':'MeshDefinition','class_5610':'PartDefinition',
           'class_5606':'CubeListBuilder','class_5603':'PartPose','class_5605':'CubeDeformation',
           'class_630':'ModelPart','class_572':'HumanoidModel',
           'method_32111':'getRoot','method_32117':'addOrReplaceChild','method_32108':'create',
           'method_32101':'texOffs','method_32097':'addBox','method_32098':'addBox',
           'method_32096':'mirror','method_32110':'create','method_32090':'offset',
           'method_32091':'offsetAndRotation','field_27701':'ZERO','field_27715':'NONE',
           'method_32011':'createMesh','m_170681_':'createMesh','m_171568_':'getChild','m_171597_':'getChild','method_32116':'getChild',
           'method_32106':'mirror','method_32103':'addBox','method_32102':'addBox',
           'f_171404_':'ZERO','method_32086':'getChild','m_171469_':'extend',
           'textureWidth':'texWidth','textureHeight':'texHeight','m_6973_':'setupAnim'}

def num(expr, env=None):
    expr=re.sub(r'\((?:float|double|int)\)', '', expr).strip()
    expr=re.sub(r'(?<=[\d.])[fFdD]\b','',expr).replace('Math.PI','pi')
    expr=expr.replace('Maths.rad','rad').replace('Math.toRadians','rad')
    def visit(n):
        if isinstance(n, ast.Constant) and isinstance(n.value,(int,float)): return n.value
        if isinstance(n, ast.Name):
            if n.id=='pi':return math.pi
            if env and n.id in env:return env[n.id]
            raise ValueError('Unknown number '+n.id)
        if isinstance(n, ast.UnaryOp) and isinstance(n.op,(ast.USub,ast.UAdd)):
            return (-1 if isinstance(n.op,ast.USub) else 1)*visit(n.operand)
        if isinstance(n, ast.BinOp):
            a,b=visit(n.left),visit(n.right)
            if isinstance(n.op,ast.Add):return a+b
            if isinstance(n.op,ast.Sub):return a-b
            if isinstance(n.op,ast.Mult):return a*b
            if isinstance(n.op,ast.Div):return a/b
        if isinstance(n,ast.Call) and isinstance(n.func,ast.Name) and n.func.id=='rad' and len(n.args)==1:return math.radians(visit(n.args[0]))
        raise ValueError('Nonconstant '+expr)
    return float(visit(ast.parse(expr,mode='eval').body))

def body(src, pattern):
    m=re.search(pattern,src)
    if not m: raise ValueError('Missing method '+pattern)
    start=src.index('{',m.start())+1;depth=1;end=start
    while depth:
        depth+=(src[end]=='{')-(src[end]=='}');end+=1
    return src[start:end-1]

def normalized(src):
    for a,b in RENAMES.items():src=src.replace(a,b)
    for key,val in re.findall(r'(?:private|public) static final String (\w+) = ("[^"]+");',src):
        src=re.sub(r'\b'+key+r'\b',val,src)
    return src

def layer(src):
    if 'class EndersoulFragmentModel' in src:
        parts=[];addpart(parts,'base','$mesh',[0,22,0,0,0,0],[-2,-2,-2,4,4,4])
        for i in range(8):addpart(parts,'stick'+str(i),'base',[0]*6,[-.5,-4 if i<4 else -6,-.5,1,8 if i<4 else 10,1],inflate=0 if i<4 else .15)
        return dict(textureSize=[64,32],parts=parts)
    if 'class EndersoulHandModel' in src:
        parts=[];addpart(parts,'hand','$mesh',[0,17.5,0,0,-.3926991,0])
        for i in range(3):
            length=6 if i==1 else 5
            addpart(parts,'finger'+str(i),'hand',[-.5,0,i-1,(i-1)*.2617994,0,.17453294 if i==1 else 0],[-.5,0,-.5,1,length,1],[i*4,0],.6)
            addpart(parts,'fore_finger'+str(i),'finger'+str(i),[0,.5+length,0,0,0,-.3926991 if i==1 else -.2617994],[-.5,0,-.5,1,length,1],[1+i*5,0],.59000003)
        addpart(parts,'thumb','hand',[.5,0,-.5,-.62831855,0,-.3926991],[-.5,0,-.5,1,5,1],[14,0],.6)
        return dict(textureSize=[32,32],parts=parts)
    text=body(src,r'public static LayerDefinition (?:createBodyLayer|createLayer|createArmorLayer|createMobHeadLayer)\(')
    if 'MutantSnowGolemModel.createBodyMesh()' in text:
        text=body(src,r'private static MeshDefinition createBodyMesh\(').replace('return meshDefinition','return LayerDefinition.create((MeshDefinition)meshDefinition, (int)128, (int)64)')
    for param in re.findall(r'CubeDeformation (\w+)\)',src):
        text=re.sub(r'\b'+param+r'\b','new CubeDeformation(0.0f)',text)
    text=re.sub(r'for \(int i = 0; i < 3; \+\+i\) \{\s*Spine.createSpineLayer\(middle, i\);\s*middle = middle.getChild\("middle" \+ \(i \+ 1\)\);\s*\}', 'recoverSpine(0);recoverSpine(1);recoverSpine(2);',text)
    deformations={}
    for key,base,extension in re.findall(r'CubeDeformation (\w+) = new CubeDeformation\(([^)]+)\)(?:\.extend\(([^)]+)\))?;',text):
        deformations[key]=num(base)+(num(extension) if extension else 0)
    text=re.sub(r'CubeDeformation \w+ = [^;]+;','',text)
    for key,value in deformations.items():text=re.sub(r'\b'+key+r'\b','new CubeDeformation('+str(value)+')',text)
    text=re.sub(r'new CubeDeformation\(([^)]+)\)\.extend\(([^)]+)\)',lambda m:'new CubeDeformation('+str(num(m[1])+num(m[2]))+')',text)
    parts=[];variables={};texture=None;anon=0
    for stmt in text.split(';'):
        stmt=stmt.strip()
        if not stmt:continue
        if re.fullmatch(r'MeshDefinition \w+ = new MeshDefinition\(\)',stmt):continue
        if re.fullmatch(r'MeshDefinition \w+ = HumanoidModel.createMesh\(.*\)',stmt):
            # Default vanilla humanoid layer inherited by owner armor/zombies.
            for name,pose,box,uv in [('head',[0,0,0,0,0,0],[-4,-8,-4,8,8,8],[0,0]),('hat',[0,0,0,0,0,0],[-4,-8,-4,8,8,8],[32,0]),('body',[0,0,0,0,0,0],[-4,0,-2,8,12,4],[16,16]),('right_arm',[-5,2,0,0,0,0],[-3,-2,-2,4,12,4],[40,16]),('left_arm',[5,2,0,0,0,0],[-1,-2,-2,4,12,4],[40,16]),('right_leg',[-1.9,12,0,0,0,0],[-2,0,-2,4,12,4],[0,16]),('left_leg',[1.9,12,0,0,0,0],[-2,0,-2,4,12,4],[0,16])]:
                addpart(parts,name,'$mesh',pose,box,uv,.5 if name=='hat' else 0,name.startswith('left'))
            continue
        m=re.fullmatch(r'PartDefinition (\w+) = \w+(?:\.getRoot\(\))?\.getChild\("([^"]+)"\)',stmt)
        if m:variables[m[1]]=m[2];continue
        nested=re.search(r'(\w+)\.getChild\("([^"]+)"\)\.addOrReplaceChild',stmt)
        if nested:
            variables['recoveredParent']=nested[2]
            stmt=stmt[:nested.start()]+'recoveredParent.addOrReplaceChild'+stmt[nested.end():]
        m=re.fullmatch(r'PartDefinition (\w+) = \w+\.getRoot\(\)',stmt)
        if m:variables[m[1]]='$mesh';continue
        m=re.fullmatch(r'return LayerDefinition.create\(\(MeshDefinition\)\w+, \(int\)(\d+), \(int\)(\d+)\)',stmt)
        if m:texture=[int(m[1]),int(m[2])];continue
        m=re.fullmatch(r'Arm.createArmLayer\((\w+), "([^"]+)", (true|false), (true|false)\)',stmt)
        if m:
            append_arm(parts,variables[m[1]],m[2],m[3]=='true',m[4]=='true');continue
        m=re.fullmatch(r'recoverSpine\((\d)\)',stmt)
        if m:
            variables['middle']=append_spine(parts,variables['middle'],int(m[1]));continue
        m=re.fullmatch(r'(?:PartDefinition (\w+) = )?(\w+)\.addOrReplaceChild\((.*)\)',stmt,re.S)
        if not m:raise ValueError('Layer statement '+stmt[:160])
        name,builder,pose=split_args(m[3]);name=json.loads(name)
        cubes=[];uv=[0,0];mirror=False
        for call,args in calls(builder.removeprefix('CubeListBuilder')):
            if call=='create':continue
            if call=='texOffs':uv=[num(x) for x in args]
            elif call=='mirror':mirror=not args or args[0]=='true'
            elif call=='addBox':
                if args[0].startswith('"'):args=args[1:]
                inf=[0,0,0]
                if len(args)==7:
                    raw=args[6].removeprefix('new CubeDeformation(').removesuffix(')')
                    inf=[num(x) for x in split_args(raw)];inf=inf*3 if len(inf)==1 else inf
                elif len(args)!=6:raise ValueError('Box '+str(args))
                cubes.append(dict(box=[num(x) for x in args[:6]],uv=uv[:],inflate=inf,mirror=mirror))
            else:raise ValueError('Builder '+call)
        if pose=='PartPose.ZERO':transform=[0]*6
        else:
            match=re.fullmatch(r'PartPose\.(offset|offsetAndRotation|rotation)\((.*)\)',pose)
            if not match:raise ValueError('Pose '+pose)
            transform=[num(x) for x in split_args(match[2])]
            if len(transform)==3:transform = [0]*3+transform if match[1]=='rotation' else transform+[0]*3
        parent=variables[m[2]]
        existing=next((p for p in parts if p['name']==name),None)
        if existing is not None and existing['parent']!=parent:name=parent+'__'+name
        parts=[p for p in parts if p['name']!=name]
        parts.append(dict(name=name,parent=parent,pose=transform,cubes=cubes))
        if m[1]:variables[m[1]]=name
    if not texture or not parts:raise ValueError('Empty layer')
    ordered=[];remaining=list(parts)
    while remaining:
        known={'$mesh'}|{p['name'] for p in ordered}
        ready=[p for p in remaining if p['parent'] in known]
        if not ready:raise ValueError('Cyclic or missing layer parent')
        ordered.extend(ready);remaining=[p for p in remaining if p not in ready]
    return dict(textureSize=texture,parts=ordered)

def addpart(parts,name,parent,pose,box=None,uv=(0,0),inflate=0,mirror=False):
    parts.append(dict(name=name,parent=parent,pose=pose,cubes=[] if box is None else [dict(box=box,uv=list(uv),inflate=[inflate]*3,mirror=mirror)]))

def append_spine(parts,parent,index):
    # Literal expansion of supplied MutantSkeletonModel.Spine.createSpineLayer.
    suffix=str(index+1);middle='middle'+suffix;scale=.98 if index==1 else 1
    addpart(parts,middle,parent,[0,-7 if index==0 else -5,0,.17453294,0,0],[-2.5,-4,-2,5,4,4],[50,0],.5)
    for side,sign in [(1,-1),(2,1)]:
        previous=middle
        for step,angle in [(1,.69813174),(2,1.0471976),(3,.8975979)]:
            key=f'side{side}{step}{suffix}'
            pose=[sign*(3 if step==1 else 6.5 if step==2 else 6.4),-1 if step==1 else 0,1.75 if step==1 else 0,0,sign*angle*scale,0]
            addpart(parts,key,previous,pose,[-6 if sign<0 else 0,-2,-2,6,2,2],[32,12],{1:.25,2:.2,3:.15}[step],(side==1 and step==2) or (side==2 and step!=2))
            previous=key
    return middle

def append_arm(parts,parent,prefix,right,lower):
    # Literal expansion of supplied MutantEndermanModel.Arm.createArmLayer.
    sign=-1 if right else 1;arm=prefix+'arm';fore=prefix+'fore_arm';hand=prefix+'hand'
    addpart(parts,arm,parent,[sign*4,-8 if lower else -14,0,-.5235988+(.1 if lower else 0),0,-sign*.5235988+(sign*.2 if lower else 0)],[-1.5,0,-1.5,3,22,3],[92,0],.1)
    addpart(parts,fore,arm,[0,21,1,-.62831855,0,0],[-1.5,0,-1.5,3,18,3],[104,0])
    addpart(parts,hand,fore,[0,17.5,0,0,sign*.3926991,0])
    for i in range(3):
        finger=prefix+'finger'+str(i);length=6 if i==1 else 5
        addpart(parts,finger,hand,[sign*.5,0,i-1,(i-1)*.2617994,0,-sign*.17453294 if i==1 else 0],[-.5,0,-.5,1,length,1],[76,0],.6,not right)
        addpart(parts,prefix+'fore_finger'+str(i),finger,[0,.5+length,0,0,0,sign*(.3926991 if i==1 else .2617994)],[-.5,0,-.5,1,length,1],[76,0],.59000003,not right)
    addpart(parts,prefix+'thumb',hand,[-sign*.5,0,-.5,-.62831855,0,sign*.3926991],[-.5,0,-.5,1,5,1],[76,0],.6,right)

def constructor(src, name, case=None):
    text=body(src,r'public '+re.escape(name)+r'\(')
    initializers='\n'.join('this.'+field+' = new '+value+';' for field,value in re.findall(r'(?:public|private|protected)(?: final)? (?:AdvancedModelBox|HideableModelBoxWithChildren) (\w+) = new ([^;]+);',src))
    text=initializers+'\n'+text
    if name in ('AtlatitanModel','LuxtructosaurusModel'):
        parent=normalized(next(INPUT.rglob('SauropodBaseModel.java')).read_text())
        text=body(parent,r'public SauropodBaseModel\(')+'\n'+text
    env={}
    params=re.search(r'public '+name+r'\(([^)]*)\)',src)[1]
    for typ,key in re.findall(r'(float|double|int) (\w+)',params):env[key]=0
    for key,value in re.findall(r'(?:float|double|int) (\w+) = ([^;]+);',text):
        try:env[key]=num(value,env)
        except (ValueError,SyntaxError):pass
    def number(x):return num(x,env)
    if name=='GumbeeperModel':text=re.sub(r'if \(f <= 0.0f\) \{([^{}]*)\}',r'\1',text)
    if name=='GalenaGauntletModel':text=re.sub(r'if \(!left\) \{([^{}]*)\} else \{([^{}]*)\}',lambda m:m[2] if case=='left' else m[1],text)
    if 'switch (' in text:
        start=text.index('switch (');switch=body(text[start:],r'switch \(')
        chosen=str(case if case is not None else ('HEAD' if name=='ModelAnaconda' else 0))
        selected=body(switch,r'case '+chosen+r': \{').replace('break;','')
        switchend=start+text[start:].index('{')+len(switch)+2
        text=text[:start]+selected+text[switchend:]
    size=[]
    for axis in ['Width','Height']:
        m=re.search(r'this.tex'+axis+r' = (\d+);',text)
        if not m:raise ValueError('Missing texture dimensions')
        size.append(int(m[1]))
    parts={};uvs={};mirrors={}
    for stmt in text.split(';'):
        stmt=stmt.strip()
        if not stmt:continue
        m=re.fullmatch(r'this\.(\w+) = new (?:AdvancedModelBox|ModelRenderer|HideableModelBoxWithChildren)\((.*)\)',stmt)
        if m:
            key=m[1];parts[key]=dict(name=key,parent='$mesh',pose=[0]*6,cubes=[])
            args=split_args(m[2]);uvs[key]=[number(x) for x in args[-2:]] if len(args)==3 else [0,0]
            continue
        m=re.fullmatch(r'this\.(\w+)\.(?:setPos|setRotationPoint)\((.*)\)',stmt)
        if m:parts[m[1]]['pose'][:3]=[number(x) for x in split_args(m[2])];continue
        m=re.fullmatch(r'this\.(\w+)\.addChild\((?:\(\w+\))?this\.(\w+)\)',stmt)
        if m:parts[m[2]]['parent']=m[1];continue
        m=re.fullmatch(r'this\.(?:setRotationAngle|setRotateAngle|setRotation)\(this\.(\w+), (.*)\)',stmt)
        if m:parts[m[1]]['pose'][3:]=[number(x) for x in split_args(m[2])];continue
        m=re.fullmatch(r'this\.(\w+)\.rotateAngle([XYZ]) = (.*)',stmt)
        if m:parts[m[1]]['pose'][3+'XYZ'.index(m[2])]=number(m[3]);continue
        m=re.fullmatch(r'this\.(\w+)\.mirror = (true|false)',stmt)
        if m:mirrors[m[1]]=m[2]=='true';continue
        m=re.fullmatch(r'this\.(\w+)(.*\.addBox\(.*\))',stmt,re.S)
        if m:
            for call,args in calls(m[2]):
                if call=='setTextureOffset':uvs[m[1]]=[number(x) for x in args]
                elif call=='addBox':
                    if len(args) not in (6,7,8):raise ValueError('Box '+str(args))
                    inf=number(args[6]) if len(args)>6 else 0
                    parts[m[1]]['cubes'].append(dict(box=[number(x) for x in args[:6]],uv=uvs[m[1]][:],inflate=[inf]*3,mirror=args[7]=='true' if len(args)==8 else mirrors.get(m[1],False)))
                else:raise ValueError(call)
            continue
        # Nongeometry constructor bookkeeping is retained in the source archive.
        if any(x in stmt for x in ['addBox','addChild','setPos','setRotation','rotateAngle','new AdvancedModelBox','new ModelRenderer']):raise ValueError('Constructor '+stmt[:170])
    ordered=[];remaining=dict(parts)
    while remaining:
        ready=[k for k,v in remaining.items() if v['parent']=='$mesh' or v['parent'] in {p['name'] for p in ordered}]
        if not ready:raise ValueError('Cyclic parts')
        for k in ready:ordered.append(remaining.pop(k))
    if not ordered:raise ValueError('Empty constructor')
    return dict(textureSize=size,parts=ordered)

def main():
    recovered={};failures={}
    for collection in ['beasts','caves','mutants','fauna','rotten']:
        for path in sorted((INPUT/collection).rglob('*.java')):
            if '/model/' not in path.as_posix() and '/models/' not in path.as_posix():continue
            src=normalized(path.read_text(encoding='utf-8'))
            if not any(x in src for x in ['new AdvancedModelBox(', 'new ModelRenderer(', 'new CubeDeformation(', 'CubeListBuilder.create()']):continue
            key=collection+'_'+path.stem.lower()
            if path.stem in ('AMModelLayers','ACModelLayers','ModModelLayers'):continue
            try:
                data=layer(src) if 'public static LayerDefinition' in src else constructor(src,path.stem)
                data.update(source=path.relative_to(INPUT).as_posix(),sourceSha256=hashlib.sha256(path.read_bytes()).hexdigest(),animationRoot='$mesh',headLook=next((p['name'] for p in data['parts'] if p['name'].lower()=='head'),''),actions={})
                if [p['name'] for p in data['parts'] if p['parent']=='$mesh']==['root']:data['animationRoot']='root'
                from recover_creature_animation import extract, assignments, poses
                # Several supplied classes inherit the complete gait from the
                # authored sauropod base; their own file contains only geometry.
                if 'extends SauropodBaseModel' in src and 'public void setupAnim(' not in src:
                    src+='\n'+normalized(next((INPUT/collection).rglob('SauropodBaseModel.java')).read_text())
                setup=re.search(r'public void setupAnim\(([^)]+)\)',src)
                if setup:
                    parameters=split_args(setup[1])
                    if len(parameters)==6:
                        for parameter,new_name in zip(parameters[1:],['limbSwing','limbSwingAmount','ageInTicks','netHeadYaw','headPitch']):
                            old_name=parameter.strip().split()[-1]
                            if old_name!=new_name:src=re.sub(r'\b'+re.escape(old_name)+r'\b',new_name,src)
                if path.stem=='ModelCaveCentipede':src=src.replace('this.type','0')
                if path.stem=='ModelAnaconda':src=src.replace('this.jaw != null && entity instanceof EntityAnaconda','true').replace('entity instanceof EntityAnacondaPart','false')
                data['actions'],data['motions'],data['unboundAnimationCalls']=extract(src,data['parts'])
                data['expressions'],missing=assignments(src,data['parts'])
                data['unboundAnimationCalls']+=missing
                data['poses']=poses(src,data['parts'])
                # These supplied classes explicitly call/inherit vanilla's
                # humanoid walk routine. Preserve those coefficients and bones.
                if path.stem in ('ModelUnderminerDwarf','GobberModel','OgreModel'):
                    bones={p['name'] for p in data['parts']}
                    for names,phase,degree in [(('rightLeg','right_leg','RightLeg'),0,1.4),(('leftLeg','left_leg','LeftLeg'),math.pi,1.4),(('rightArm','right_arm','RightArm'),math.pi,1.4 if path.stem!='ModelUnderminerDwarf' else 1),(('leftArm','left_arm','LeftArm'),0,1.4 if path.stem!='ModelUnderminerDwarf' else 1)]:
                        bone=next((n for n in names if n in bones),None)
                        if bone:data['expressions'].append(dict(bone=bone,target='xRot',op='=',value=['*',['*',['cos',['+',['*','walk',.6662],phase]],degree],'move']))
                if path.stem=='ScarabModel':
                    from recover_creature_animation import scarab_expressions
                    data['expressions']=scarab_expressions(next((INPUT/'rotten').rglob('ScarabAnimations.java')).read_text())
                recovered[key]=data;write(OUT/(key+'.json'),data)
                if path.stem in ('ModelAnaconda','ModelCaveCentipede'):
                    for variant in (['NECK','BODY','TAIL'] if path.stem=='ModelAnaconda' else [1,2]):
                        extra=constructor(src,path.stem,variant)
                        extra.update(source=data['source'],sourceSha256=data['sourceSha256'],animationRoot='$mesh',headLook='',actions={},motions=[],expressions=[])
                        if path.stem=='ModelCaveCentipede':
                            offset=['*',['+','segment',1],math.pi*.5]
                            for part in extra['parts']:
                                n=part['name'];rear=n.endswith('B') or n.endswith('B2');phase=['+',offset,.5] if rear else offset
                                if 'Leg' not in n:continue
                                flap='Leg2' in n or n.endswith('F2') or n.endswith('B2')
                                extra['motions'].append(dict(bone=n,kind='flap' if flap else 'swing',speed=1.5,degree=.85*(.5 if flap else 1),invert=n.startswith('left'),phase=phase,bias=.1 if flap else 0,clock='walk',amount='move'))
                            torso='tail' if variant==2 else 'body';walkoffset=['*',offset,math.pi*.5]
                            extra['expressions']=[dict(bone=torso,target='y',op='+=',value=['+',['-',['*',['*',['sin',['-',['*','walk',1.5],walkoffset]],'move'],.85],['*','move',.85]],['*',['sin',['-',['*','age',.1],walkoffset]],.01]])]
                            if variant==2:
                                for n,left in [('leftTail',True),('rightTail',False)]:
                                    extra['motions']+= [dict(bone=n,kind='swing',speed=1.5,degree=.17,invert=left,phase=['+',offset,1],bias=0,clock='walk',amount='move'),dict(bone=n,kind='walk',speed=.25,degree=.35,invert=left,phase=['+',offset,1.5],bias=-.5 if left else .5,clock='age',amount=1)]
                        write(OUT/(key+'_'+str(variant).lower()+'.json'),extra)
                if path.stem=='GalenaGauntletModel':
                    extra=constructor(src,path.stem,'left');extra.update(source=data['source'],animationRoot='$mesh',headLook='',actions={});write(OUT/(key+'_left.json'),extra)
            except (ValueError,KeyError,SyntaxError,IndexError) as e:failures[key]=str(e)
    write(REPORT/'geometry-recovery.json',dict(recovered={k:dict(source=v['source'],parts=len(v['parts'])) for k,v in recovered.items()},unsupported=failures))
    # Supplied renderers explicitly delegate these variants to vanilla models.
    parts=[]
    addpart(parts,'head','$mesh',[0,6,0,0,0,0],[-4,-8,-4,8,8,8],[0,0])
    addpart(parts,'body','$mesh',[0,6,0,0,0,0],[-4,0,-2,8,12,4],[16,16])
    for name,x,z in [('right_hind_leg',-2,4),('left_hind_leg',2,4),('right_front_leg',-2,-4),('left_front_leg',2,-4)]:
        addpart(parts,name,'$mesh',[x,18,z,0,0,0],[-2,0,-2,4,6,4],[0,16])
    gait=[dict(bone=n,target='xRot',op='=',value=['*',['*',['cos',['+',['*','walk',.6662],phase]],1.4],'move']) for n,phase in [('right_hind_leg',0),('left_hind_leg',math.pi),('right_front_leg',math.pi),('left_front_leg',0)]]
    write(OUT/'mutants_creeperminionmodel.json',dict(textureSize=[64,32],parts=parts,animationRoot='$mesh',headLook='head',actions={},motions=[],expressions=gait,vanillaCreeperMinion=True,source='CreeperMinionModel extends vanilla CreeperModel'))
    parts=[]
    for name,pose,box,uv in [('head',[0,0,0,0,0,0],[-4,-8,-4,8,8,8],[0,0]),('body',[0,0,0,0,0,0],[-4,0,-2,8,12,4],[16,16]),('right_arm',[-5,2,0,0,0,0],[-3,-2,-2,4,12,4],[40,16]),('left_arm',[5,2,0,0,0,0],[-1,-2,-2,4,12,4],[32,48]),('right_leg',[-1.9,12,0,0,0,0],[-2,0,-2,4,12,4],[0,16]),('left_leg',[1.9,12,0,0,0,0],[-2,0,-2,4,12,4],[16,48])]:
        addpart(parts,name,'$mesh',pose,box,uv)
    write(OUT/'beasts_underminerhumanoid.json',dict(textureSize=[64,64],parts=parts,animationRoot='$mesh',headLook='head',actions={},motions=[],expressions=[dict(bone=n,target='xRot',op='=',value=['*',['*',['cos',['+',['*','walk',.6662],phase]],degree],'move']) for n,phase,degree in [('right_leg',0,1.4),('left_leg',math.pi,1.4),('right_arm',math.pi,1),('left_arm',0,1)]],source='RenderUnderminer vanilla humanoid variant'))
    print('Recovered',len(recovered),'models; unsupported',len(failures))
    for k,v in failures.items():print(k,v)

if __name__=='__main__':main()
