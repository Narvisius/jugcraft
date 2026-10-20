"""Static conversion of owner-authored Citadel keyframes and locomotion calls.

All unsupported/dynamic expressions are reported explicitly. No bytecode or
source programs from the supplied collections are executed.
"""
import math
import re
import ast
from recover_creature_models import num, body, split_args

def condition(raw, locals=None):
    """Translate only states the native entity actually exposes. Unknown is an error."""
    raw=re.sub(r'(?<=[\d.])[fFdD]\b','',raw)
    raw=raw.replace('this.f_102610_','baby').replace('this.young','baby')
    for pattern,state in [(r'\w+\.isFreddy\(\)','freddy'),(r'\w+\.isEating\(\)','eat'),
                          (r'\w+\.(?:isBaby|m_6162_)\(\)','baby'),
                          (r'\w+\.(?:isInWater|m_20069_)\(\)','swim'),
                          (r'\w+\.isFlying\(\)','fly'),(r'\w+\.(?:isRunning|isSprinting)\(\)','run')]:raw=re.sub(pattern,state,raw)
    raw=raw.replace('&&',' and ').replace('||',' or ')
    raw=re.sub(r'!(?!=)',' not ',raw).strip()
    variables={'sitProgress':['*','sit',5],'swimProgress':['*','swim',5],
               'flyProgress':['*','fly',5],'standProgress':'stand','sleepProgress':['*','sleep',5],'running':'run','runProgress':['*','run',5],'chargeProgress':['*','charge',5],'true':1,'false':0}
    variables.update(locals or {})
    def convert(n):
        if isinstance(n,ast.BoolOp):
            values=[convert(v) for v in n.values];result=values[0]
            for value in values[1:]:result=['&&' if isinstance(n.op,ast.And) else '||',result,value]
            return result
        if isinstance(n,ast.UnaryOp) and isinstance(n.op,ast.Not):return ['!',convert(n.operand)]
        if isinstance(n,ast.Compare) and len(n.ops)==1:
            return [{ast.Eq:'==',ast.NotEq:'!=',ast.Lt:'<',ast.LtE:'<=',ast.Gt:'>',ast.GtE:'>='}[type(n.ops[0])],convert(n.left),convert(n.comparators[0])]
        return expression(ast.unparse(n),variables|{s:s for s in ('sit','swim','fly','baby','freddy','eat','run','sleep')})
    return convert(ast.parse(raw,mode='eval').body)

def scopes(setup, variables=None):
    """Keep enclosing if/else conditions attached to source calls."""
    result=[];stack=[];previous=None;offset=0
    for line in setup.splitlines(keepends=True):
        stripped=line.strip()
        if stripped.startswith('}'):
            previous=stack.pop() if stack else None
        match=re.search(r'\bif\s*\((.*)\)\s*\{',line)
        if match:
            try:guard=condition(match[1],variables)
            except (ValueError,SyntaxError,KeyError):guard={'unsupported':match[1]}
            chain=guard
            if 'else' in line and previous is not None:
                guard=['&&',['!',previous[1]],guard];chain=['||',previous[1],chain]
            stack.append((guard,chain))
        elif re.search(r'\belse\s*\{',line):
            guard=['!',previous[1]] if previous is not None else {'unsupported':'else without if'}
            stack.append((guard,1))
        # Array initializers and faceTarget arguments contain balanced braces;
        # they do not open a control-flow scope.
        elif line.count('{')>line.count('}'):stack.append(({'unsupported':stripped},1))
        result.append((offset,offset+len(line),[frame[0] for frame in stack]))
        offset+=len(line)
    return result

def guard_at(scopes,offset):
    guards=next((s for start,end,s in scopes if start<=offset<end),[])
    if any('unsupported' in str(g) for g in guards):raise ValueError('Unbound source condition '+str(guards))
    result=1
    for g in guards:result=['&&',result,g]
    return result

def expression(raw, variables):
    raw=re.sub(r'\((?:float|double|int)\)','',raw)
    raw=re.sub(r'(?<=[\d.])[fFdD]\b','',raw).replace('Math.PI',str(math.pi))
    for a,b in [('MathHelper.func_76134_b','cos'),('Mth.m_14089_','cos'),('Mth.m_14031_','sin'),('Mth.cos','cos'),('Mth.sin','sin'),('Math.cos','cos'),('Math.sin','sin'),('Math.abs','abs'),('Math.min','min'),('Math.max','max'),('ACMath.walkValue','walkValue'),('Math.toRadians','radians'),('Maths.rad','radians'),('Mth.m_14036_','clamp'),('Mth.clamp','clamp')]:raw=raw.replace(a,b)
    for pattern,state in [(r'\w+\.(?:isInWater|m_20069_)\(\)','swim'),(r'\w+\.(?:getSwimAmount|m_20998_)\([^)]*\)','swim'),(r'\w+\.isFlying\(\)','fly')]:raw=re.sub(pattern,state,raw)
    for pattern,state in [(r'\w+\.getPropellerAngle\([^)]*\)','propeller'),(r'\w+\.get(?:Fish|Jelly|Bird)Pitch\([^)]*\)','pitch')]:raw=re.sub(pattern,state,raw)
    ternary=re.fullmatch(r'(.+?)\?(.+?):(.+)',raw)
    if ternary:return ['select',condition(ternary[1].strip(),variables),expression(ternary[2],variables),expression(ternary[3],variables)]
    for a,b in [('state.walkAnimationPos','walk'),('state.walkAnimationSpeed','move'),('state.ageInTicks','age'),('state.xRot','pitch'),('state.yRot','yaw')]:raw=raw.replace(a,b)
    def convert(n):
        if isinstance(n,ast.Constant) and isinstance(n.value,(int,float)):return n.value
        if isinstance(n,ast.Name):
            if n.id in variables:return variables[n.id]
            if n.id in ('walk','move','age','pitch','yaw','swim','fly','ground','stand','propeller','body_yaw'):return n.id
            if n.id in ('true','false'):return int(n.id=='true')
            raise ValueError('Unbound expression variable '+n.id)
        if isinstance(n,ast.UnaryOp) and isinstance(n.op,ast.USub):return ['*',-1,convert(n.operand)]
        if isinstance(n,ast.BinOp):
            op={ast.Add:'+',ast.Sub:'-',ast.Mult:'*',ast.Div:'/'}[type(n.op)]
            return [op,convert(n.left),convert(n.right)]
        if isinstance(n,ast.Call) and isinstance(n.func,ast.Name) and n.func.id in ('cos','sin','abs','min','max'):return [n.func.id]+[convert(a) for a in n.args]
        if isinstance(n,ast.Call) and isinstance(n.func,ast.Name) and n.func.id=='clamp':return ['max',convert(n.args[1]),['min',convert(n.args[0]),convert(n.args[2])]]
        if isinstance(n,ast.Call) and isinstance(n.func,ast.Name) and n.func.id=='radians':return ['*',convert(n.args[0]),math.pi/180]
        if isinstance(n,ast.Call) and isinstance(n.func,ast.Name) and n.func.id=='walkValue':
            clock,amount,speed,offset,degree,invert=[convert(a) for a in n.args]
            return ['*',['*',['cos',['+',['*',clock,speed],offset]],['*',degree,amount]],['select',invert,-1,1]]
        raise ValueError('Unsupported expression '+raw)
    return convert(ast.parse(raw.strip(),mode='eval').body)

def assignments(src, parts):
    """Direct source expressions used by vanilla-style models, no source eval."""
    variables={'limbSwing':'walk','limbSwingAmount':'move','ageInTicks':'age','netHeadYaw':'yaw','headPitch':'pitch','f':'walk','f1':'move','f2':'age','f3':'yaw','f4':'pitch'}
    aliases={p['name']:p['name'] for p in parts};result=[];unsupported=[]
    for field,expr in re.findall(r'this\.(\w+) = ([^;]*getChild[^;]+);',src):
        children=re.findall(r'getChild\((?:\(String\))?"([^"]+)"\)',expr)
        if children and children[-1] in aliases:aliases[field]=children[-1]
    try:setup=body(src,r'public void (?:setupAnim|func_78087_a)\(')
    except ValueError:
        if 'extends ModelQuadruped' in src:
            for bone,phase in [('field_78149_c',0),('field_78146_d',math.pi),('field_78147_e',math.pi),('field_78144_f',0)]:
                if bone in aliases:result.append(dict(bone=bone,target='xRot',op='=',value=['*',['*',['cos',['+',['*','walk',.6662],phase]],1.4],'move']))
            if 'field_78150_a' in aliases:
                for axis,state in [('xRot','pitch'),('yRot','yaw')]:result.append(dict(bone='field_78150_a',target=axis,op='=',value=['*',state,math.pi/180]))
        return result,[]
    variables=runtime_locals(setup,variables);contexts=scopes(setup,variables);offset=0
    for line in setup.splitlines(keepends=True):
        position=offset;offset+=len(line)
        try:guard=guard_at(contexts,position)
        except ValueError:continue
        if '{' in line or '}' in line:continue
        local=re.search(r'float (\w+) = (.*);',line)
        if local:
            try:variables[local[1]]=expression(local[2],variables)
            except (ValueError,KeyError,SyntaxError):variables.pop(local[1],None)
        scale=re.search(r'this\.(\w+)\.setScale\((.*)\);',line)
        if scale and scale[1] in aliases:
            try:
                values=[expression(arg,variables) for arg in split_args(scale[2])]
                for target,value in zip(('xScale','yScale','zScale'),values):result.append(dict(bone=aliases[scale[1]],target=target,op='=',value=value,when=guard))
            except (ValueError,KeyError,SyntaxError) as e:unsupported.append(dict(call=scale[0],reason=str(e)))
        m=re.search(r'this\.(\w+)\.(xRot|yRot|zRot|rotateAngleX|rotateAngleY|rotateAngleZ|f_104203_|f_104204_|f_104205_|rotationPointX|rotationPointY|rotationPointZ|x|y|z) (\+=|-=|=) (.*);',line)
        if not m:continue
        if m[1] not in aliases:continue
        target={'rotateAngleX':'xRot','rotateAngleY':'yRot','rotateAngleZ':'zRot','f_104203_':'xRot','f_104204_':'yRot','f_104205_':'zRot','rotationPointX':'x','rotationPointY':'y','rotationPointZ':'z'}.get(m[2],m[2])
        try:
            value=expression(m[4],variables)
            # TurkeyRenderer supplies a bounded wing-flap value as ageInTicks.
            if 'class ModelTurkey' in src and m[1] in ('leftWing','rightWing'):
                value=['*',1 if m[1]=='rightWing' else -1,['*','fly',['+',['sin',['*','age',1.7]],1]]]
            result.append(dict(bone=aliases[m[1]],target=target,op=m[3],value=value,when=guard))
        except (ValueError,KeyError,SyntaxError) as e:unsupported.append(dict(call=m[0],reason=str(e)))
    return result,unsupported

def runtime_locals(setup,variables):
    """Resolve authored arithmetic from synchronized native movement states."""
    variables=dict(variables)
    for local,raw in re.findall(r'(?:float|boolean) (\w+) = ([^;]+);',setup):
        if re.fullmatch(r'\w+\.getRunAmount\([^)]*\)',raw):variables[local]='run';continue
        if local=='bodyYaw' and 'entity.f_20884_' in raw:variables[local]='body_yaw';continue
        if 'prevFlapAmount' in raw:
            raw=re.sub(r'\w+\.(?:prevFlapAmount|flapAmount)', 'flap',raw)
            variables['flap']='flap';variables['partialTick']=variables['partialTicks']=0
        if 'prevBirdPitch' in raw:
            raw=re.sub(r'\w+\.(?:prevBirdPitch|birdPitch)', 'motion_pitch',raw)
            variables['motion_pitch']='motion_pitch';variables['partialTick']=variables['partialTicks']=0
        if 'prevSwoopProgress' in raw:
            raw=re.sub(r'\w+\.(?:prevSwoopProgress|swoopProgress)', 'swoop',raw)
            variables['swoop']='swoop';variables['partialTick']=variables['partialTicks']=0
        state=None
        for word,value in [('sit','sit'),('swim','swim'),('fly','fly'),('flight','fly'),('glide','fly'),('sleep','sleep'),('run','run'),('sprint','run'),('(?:on)?land','land'),('ground','ground'),('stand','stand'),('explode','charge'),('charge','charge'),('mouthOpen','combat'),('attack','combat'),('jump','jump'),('leap','jump'),('squish','squish')]:
            if re.search(r'\b\w+\.(?:prev)?'+word+r'\w*Progress\b|\.get'+word+r'\w*Progress\(',raw,re.I):state=value;break
        if state:
            maximum=5
            use=re.search(r'progress(?:Rotation|Position)Prev\([^;]*?, '+re.escape(local)+r', [^;]+?, ([\d.]+)f\);',setup)
            if use:maximum=float(use[1])
            # Entity getter progress values are already normalized to [0,1].
            if re.search(r'\.get\w+Progress\(',raw):maximum=1
            variables[local]=['*',state,maximum]
        else:
            if 'getJiggleTime(' in raw or 'entity.squishFactor' in raw:variables[local]='squish';continue
            try:variables[local]=expression(raw,variables)
            except (ValueError,KeyError,SyntaxError):variables.pop(local,None)
    return variables

def extract(src, parts):
    bones={p['name'] for p in parts}; clips={};unbound=[];waves=[]
    constants={}
    for name,expr in re.findall(r'float (\w+) = ([^;]+);',src):
        try:constants[name]=num(expr,constants)
        except (ValueError,SyntaxError,ZeroDivisionError):pass
    timeline=None;pose={};previous={};channels={};time=0;duration=0;clipname='';active=False
    def endframe(reset=False):
        nonlocal time,previous,pose
        time+=duration/20
        for key in previous.keys()|pose.keys():
            frames=channels.setdefault(key,[dict(time=0,vector=[0,0,0],interpolation='LINEAR')])
            if frames[-1]['time']<time-duration/20:
                frames.append(dict(time=time-duration/20,vector=previous.get(key,[0,0,0]),interpolation='LINEAR'))
            frames.append(dict(time=time,vector=pose.get(key,[0,0,0]),interpolation='LINEAR'))
        previous=pose;pose={}
    def save():
        if clipname and channels:
            clips[clipname]=dict(length=time,loop=False,channels=[dict(bone=k[0],target=k[1],frames=v) for k,v in channels.items()])
    # Calls are parsed from their balanced parentheses, including Maths.rad.
    for match in re.finditer(r'this\.animator\.(setAnimation|startKeyframe|endKeyframe|resetKeyframe|setStaticKeyframe|rotate|move)\(',src):
        begin=match.end();end=begin;depth=1
        while depth:
            depth+=(src[end]=='(')-(src[end]==')');end+=1
        args=split_args(src[begin:end-1]);call=match[1]
        try:
            if call=='setAnimation':
                save();clipname=args[0].split('.')[-1].removeprefix('ANIMATION_').lower();time=0;pose={};previous={};channels={};duration=0
            elif call=='startKeyframe':duration=num(args[0],constants);pose={};active=True
            elif call=='endKeyframe':
                if active:endframe();active=False
            elif call in ('resetKeyframe','setStaticKeyframe'):
                duration=num(args[0],constants);pose={} if call=='resetKeyframe' else dict(previous);endframe();active=False
            else:
                bone=args[0].removeprefix('this.');values=[num(x,constants) for x in args[1:]]
                if bone not in bones:raise ValueError('Unknown bone '+bone)
                if call=='rotate':values=[math.degrees(x) for x in values]
                else:values[1]=-values[1] # KeyframeAnimations.posVec flips Y; Citadel move does not.
                key=(bone,'ROTATION' if call=='rotate' else 'POSITION')
                pose[key]=[a+b for a,b in zip(pose.get(key,[0,0,0]),values)]
        except (ValueError,SyntaxError,ZeroDivisionError) as e:unbound.append(dict(call=src[match.start():end],reason=str(e)))
    save()
    # Recover locomotion parameters, retaining exact source speed, amplitude,
    # phase, bias and inversion. Runtime state supplies distance/elapsed time.
    try:setup=body(src,r'public void setupAnim\(')
    except ValueError:setup=''
    seen=set();contexts=scopes(setup)
    dynamic=constants|{'limbSwing':'walk','limbSwingAmount':'move','ageInTicks':'age','netHeadYaw':'yaw','headPitch':'pitch'}
    # Recover runtime arithmetic around the same gait values instead of dropping
    # authored movement merely because its amplitude is state dependent.
    dynamic=runtime_locals(setup,dynamic);contexts=scopes(setup,dynamic)
    for match in re.finditer(r'this\.(walk|swing|flap|bob)\(([^;]+)\);',setup):
        call=match[1];args=split_args(match[2]);bone=args[0].removeprefix('this.')
        try:
            guard=guard_at(contexts,match.start())
            if bone not in bones:raise ValueError('Unknown bone '+bone)
            speed=expression(args[1],dynamic);degree=expression(args[2],dynamic)
            invert=args[3]=='true'
            if args[3] not in ('true','false'):raise ValueError('Dynamic inversion')
            phase=0 if call=='bob' else expression(args[4],dynamic)
            bias=0 if call=='bob' else expression(args[5],dynamic)
            clock=expression(args[-2],dynamic);amount=expression(args[-1],dynamic)
            # Multiple authored waves on the same bone are additive. Their
            # enclosing conditions, rather than deduplication, choose the gait.
            waves.append(dict(bone=bone,kind=call,speed=speed,degree=degree,invert=invert,phase=phase,bias=bias,clock=clock,amount=amount,when=guard))
        except (ValueError,KeyError,SyntaxError,ZeroDivisionError) as e:unbound.append(dict(call=match[0],reason=str(e)))
    for match in re.finditer(r'this\.(chainSwing|chainWave|chainFlap)\(([^;]+)\);',setup):
        args=split_args(match[2]);array=args[0]
        if not array.startswith('new '):
            declaration=re.search(r'\b'+re.escape(array)+r' = new \w+\[\]\{([^}]+)\}',setup)
            array=declaration[1] if declaration else ''
        else:array=array[array.find('{')+1:array.rfind('}')]
        try:
            guard=guard_at(contexts,match.start())
            speed,degree,phase=[expression(x,dynamic) for x in args[1:4]]
            clock=expression(args[4],dynamic);amount=expression(args[5],dynamic)
            names=[x.strip().removeprefix('this.') for x in array.split(',')]
            if not names or any(n not in bones for n in names):raise ValueError('Unresolved chain bones')
            for index,bone in enumerate(names):waves.append(dict(bone=bone,kind={'chainSwing':'swing','chainWave':'walk','chainFlap':'flap'}[match[1]],speed=speed,degree=degree,invert=False,phase=['*',phase,index*math.pi/(2*len(names))],bias=0,clock=clock,amount=amount,when=guard))
        except (ValueError,KeyError,SyntaxError):unbound.append(dict(call=match[0],reason='Unbound chain motion'))
    return clips,waves,unbound

def poses(src, parts):
    result=[];bones={p['name'] for p in parts}
    try:setup=body(src,r'public void setupAnim\(')
    except ValueError:return result
    variables=runtime_locals(setup,{'limbSwing':'walk','limbSwingAmount':'move','ageInTicks':'age'})
    for m in re.finditer(r'this\.(progressRotationPrev|progressPositionPrev)\(([^;]+)\);',setup):
        args=split_args(m[2]);bone=args[0].removeprefix('this.')
        if bone not in bones:continue
        try:
            guard=guard_at(scopes(setup,variables),m.start())
            values=[num(x) for x in args[2:5]]
            weight=['/',expression(args[1],variables),num(args[5])]
        except (ValueError,SyntaxError):continue
        result.append(dict(bone=bone,weight=weight,target='rotation' if m[1]=='progressRotationPrev' else 'position',value=values,when=guard))
    return result

def scarab_expressions(src):
    """Recover both supplied MathAnimator expression graphs (no sampled remake)."""
    result=[]
    for mode,sequence in re.findall(r'MathAnimator (FLYING|WALK) = (.*?);',src,re.S):
        for channel in re.finditer(r'addAnimation\("([^"]+)", (.*?)(?=\.addAnimation|\.build)',sequence,re.S):
            for point in re.finditer(r'new AnimatedPoint\(class_7179.class_7183.(field_3788[67]), animTime -> new Vector3f\(',channel[2]):
                start=point.end();end=start;depth=1
                while depth:
                    depth+=(channel[2][end]=='(')-(channel[2][end]==')');end+=1
                args=split_args(channel[2][start:end-1]);rotation=point[1]=='field_37887'
                for axis,raw in zip(('xRot','yRot','zRot') if rotation else ('x','y','z'),args):
                    raw=raw.replace('animTime.floatValue()','(age / 20.0)')
                    raw=re.sub(r'ScarabAnimations.toRadians\(([^)]+)\)',lambda m:str(math.radians(num(m[1]))),raw)
                    for a,b in [('class_3532.method_15362','cos'),('class_3532.method_15374','sin'),('class_3532.method_15379','abs')]:raw=raw.replace(a,b)
                    result.append(dict(bone=channel[1],target=axis,op='+=',value=expression(raw,{}),when='fly' if mode=='FLYING' else ['&&',['!','fly'],['>','move',.01]]))
    return result
