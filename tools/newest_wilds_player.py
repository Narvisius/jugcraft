"""Retarget supplied axe, ice and solar gestures to the approved native humanoid animation layer.

Samples numeric keyframes and the four authored sine expressions as animation
data. Never imports/executes supplied code. Clavicle/waist joints are composed
into vanilla joints; original custom-rig controller and item bones are recorded
as limitations rather than silently claimed to have exact native equivalents.
"""
import math,re,ast
from newest_building import SOURCE,RES,CAT,read,write

def number(v,t):
 if isinstance(v,(int,float)):return v
 # Interpret only arithmetic animation scalars; never eval supplied expressions.
 def scalar(node):
  if isinstance(node,ast.Constant) and isinstance(node.value,(int,float)):return node.value
  if isinstance(node,ast.Attribute) and isinstance(node.value,ast.Name) and node.value.id=='query' and node.attr=='anim_time':return t
  if isinstance(node,ast.UnaryOp) and isinstance(node.op,(ast.USub,ast.UAdd)):return (-1 if isinstance(node.op,ast.USub) else 1)*scalar(node.operand)
  if isinstance(node,ast.BinOp):
   a,b=scalar(node.left),scalar(node.right)
   if isinstance(node.op,ast.Add):return a+b
   if isinstance(node.op,ast.Sub):return a-b
   if isinstance(node.op,ast.Mult):return a*b
   if isinstance(node.op,ast.Div):return a/b
  if isinstance(node,ast.Call) and not node.keywords and len(node.args)==1 and isinstance(node.func,ast.Attribute) and isinstance(node.func.value,ast.Name) and node.func.value.id=='math' and node.func.attr in ['sin','cos']:
   return (math.sin if node.func.attr=='sin' else math.cos)(math.radians(scalar(node.args[0])))
  raise ValueError('Unsupported animation scalar '+v)
 return scalar(ast.parse(v,mode='eval').body)

def ease(name,x):
 if name in ['linear',None]:return x
 mode=re.fullmatch(r'ease(InOut|In|Out)(Quad|Cubic|Quart|Quint|Sine|Expo)',name)
 if not mode:raise ValueError('Unknown authored easing '+name)
 def power(v):
  family=mode[2]
  if family=='Sine':return 1-math.cos(v*math.pi/2)
  if family=='Expo':return 0 if v==0 else 2**(10*v-10)
  return v**{'Quad':2,'Cubic':3,'Quart':4,'Quint':5}[family]
 return power(x) if mode[1]=='In' else 1-power(1-x) if mode[1]=='Out' else power(2*x)/2 if x<.5 else 1-power(2-2*x)/2

def sample(channel,t):
 if channel is None:return [0,0,0]
 if isinstance(channel,list):return [number(v,t) for v in channel]
 if 'vector' in channel:return [number(v,t) for v in channel['vector']]
 keys=sorted((float(k),v) for k,v in channel.items())
 if t<=keys[0][0]:return sample(keys[0][1],t)
 for (a,av),(b,bv) in zip(keys,keys[1:]):
  if t<=b:
   mix=ease(bv.get('easing','linear'),(t-a)/(b-a));left=sample(av,t);right=sample(bv,t)
   return [x+(y-x)*mix for x,y in zip(left,right)]
 return sample(keys[-1][1],t)

def multiply(a,b):return [[sum(a[i][k]*b[k][j] for k in range(3)) for j in range(3)] for i in range(3)]
def matrix(v):
 x,y,z=map(math.radians,v);cx,sx=math.cos(x),math.sin(x);cy,sy=math.cos(y),math.sin(y);cz,sz=math.cos(z),math.sin(z)
 return [[cz*cy,cz*sy*sx-sz*cx,cz*sy*cx+sz*sx],[sz*cy,sz*sy*sx+cz*cx,sz*sy*cx-cz*sx],[-sy,cy*sx,cy*cx]]
def rotations(bones,names,t):
 m=[[1,0,0],[0,1,0],[0,0,1]]
 for n in names:m=multiply(m,matrix(sample(bones.get(n,{}).get('rotation'),t)))
 y=math.asin(max(-1,min(1,-m[2][0])))
 if abs(math.cos(y))<1e-6:return [math.atan2(-m[0][1],m[1][1]),y,0]
 return [math.atan2(m[2][1],m[2][2]),y,math.atan2(m[1][0],m[0][0])]

def first_person():
 source=read(SOURCE/'jugziesmobs/assets/mowziesmobs/animations/animated_player_first_person.animation.json')['animations']
 result={}
 names={'axe_sweep_right':'axe_swing_start_right','axe_sweep_left':'axe_swing_start_right','axe_slam':'axe_swing_vertical'}
 names.update({n:n for n in ['solar_beam_charge','sunstrike','solar_flare','supernova','ice_breath_start','ice_breath_loop','ice_breath_end']})
 for target,original in names.items():
  clip=source[original];length=clip['animation_length'];arms={}
  for side in ['Right','Left']:
   bone=clip['bones'].get(side+'Arm',{});controller=clip['bones'].get('FixedPitchController'+side,{})
   rows=[]
   for i in range(math.ceil(length*80)+1):
    t=min(i/80,length)
    rows.append(sample(bone.get('rotation'),t)+sample(bone.get('position'),t)+sample(bone.get('scale',[1,1,1]),t)+[sample(controller.get('position'),t)[0]])
   arms[side.lower()]=rows
  result[target]={'length':length*20,'loop':target=='ice_breath_loop','arms':arms}
 write(RES/'assets/jugcraft/wilds_first_person.json',result)

def main():
 source=read(SOURCE/'jugziesmobs/assets/mowziesmobs/animations/animated_player.animation.json')['animations']
 chains={'body':['Root','Waist','Body'],'head':['Root','Waist','Body','Neck','Head'],'rightArm':['Root','Waist','Body','RightClavicle','RightArm'],'leftArm':['Root','Waist','Body','LeftClavicle','LeftArm'],'rightLeg':['Root','RightHip','RightLeg'],'leftLeg':['Root','LeftHip','LeftLeg']}
 records=[]
 for original,target in [('axe_swing_start_right','wilds_axe_sweep_right'),('axe_swing_start_left','wilds_axe_sweep_left'),('axe_swing_vertical','wilds_axe_slam')]+[(n,'wilds_'+n) for n in ['solar_beam_charge','sunstrike','solar_flare','supernova','ice_breath_start','ice_breath_loop','ice_breath_end']]:
  clip=source[original];bones=clip['bones'];moves=[];previous={};ticks=math.ceil(clip['animation_length']*20)
  for tick in range(ticks+1):
   t=min(tick/20,clip['animation_length'])
   for joint,chain in chains.items():
    values=rotations(bones,chain,t)
    if joint in previous:values=[v+round((old-v)/(2*math.pi))*2*math.pi for v,old in zip(values,previous[joint])]
    previous[joint]=values
    moves.append({'tick':tick,'easing':'LINEAR','turn':0,joint:dict(zip(['pitch','yaw','roll'],values))})
  write(RES/'assets/jugcraft/player_animations'/(target+'.json'),{'name':target,'author':'Owner-supplied Jugcraft library; native retarget by OpenAI Codex','description':'Original '+original+' retargeted to native humanoid joints','emote':{'isLoop':'true' if original=='ice_breath_loop' else 'false','returnTick':0,'beginTick':0,'endTick':ticks,'stopTick':ticks+3,'degrees':False,'moves':moves}})
  records.append({'source':original,'runtime':target,'durationTicks':ticks,'retained':'Original numeric rotations, easing and authored sine motion; composed source joint chains','limitations':['Custom rig joint translations and held-item bones not yet retargeted','Original first-person arm transforms are sampled separately and bound through WildFirstPerson; visual fit remains unvalidated']})
 first_person()
 write(CAT/'wilds-player-animation-retarget.json',records)
 print('Retargeted '+str(len(records))+' original player gestures; in-game presentation unvalidated.')
if __name__=='__main__':main()
