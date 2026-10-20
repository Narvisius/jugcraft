"""Block-local collision boxes for supplied vanilla model geometry and multipart states."""
import itertools
import math


def model_boxes(model, elements):
    if isinstance(model,list):model=model[0]
    result=[]
    for element in elements(model['model']):
        corners=list(itertools.product(*zip(element['from'],element['to'])))
        if any(abs(element['from'][i]-element['to'][i])<.2 for i in range(3)):continue
        def rotate(points,axis,angle,origin):
            radians=math.radians(angle);c,s=math.cos(radians),math.sin(radians)
            a,b={'x':(1,2),'y':(2,0),'z':(0,1)}[axis];out=[]
            for point in points:
                p=[point[i]-origin[i] for i in range(3)]
                p[a],p[b]=p[a]*c-p[b]*s,p[a]*s+p[b]*c
                out.append([p[i]+origin[i] for i in range(3)])
            return out
        if 'rotation' in element:
            r=element['rotation'];corners=rotate(corners,r['axis'],r['angle'],r['origin'])
        # Model state rotations are clockwise in Minecraft's coordinate convention.
        corners=rotate(corners,'x',-model.get('x',0),[8,8,8])
        corners=rotate(corners,'y',-model.get('y',0),[8,8,8])
        lo=[max(0,min(16,min(p[i] for p in corners))) for i in range(3)]
        hi=[max(0,min(16,max(p[i] for p in corners))) for i in range(3)]
        if all(hi[i]>lo[i] for i in range(3)):result.append(lo+hi)
    return result


def multipart_shapes(state,elements):
    return [{'when':part.get('when',{}),'boxes':model_boxes(part['apply'],elements)} for part in state.get('multipart',[])]
