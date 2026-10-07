"""Original procedural ghost/suit sounds (MIT). No sampled recordings or external downloads.
Requires ffmpeg on PATH. Regenerates only the scary/ sound assets.
"""
from pathlib import Path
import array, json, math, random, subprocess, tempfile, wave
ROOT=Path(__file__).resolve().parents[1]
ASSETS=ROOT/'src/main/resources/assets/jugcraft'
sounds=json.loads((ASSETS/'sounds.json').read_text(encoding='utf-8'))
names=sorted({s['name'].split(':',1)[1] for e in sounds.values() for s in e['sounds'] if isinstance(s,dict) and s.get('name','').startswith('jugcraft:scary/')})
rate=22050
for index,name in enumerate(names):
    rng=random.Random(901+index)
    metal=any(k in name for k in ['step','chain'])
    laugh=any(k in name for k in ['laugh','bkmedieval'])
    duration=.45 if metal else (2.3 if laugh else 1.25)
    data=array.array('h')
    for i in range(int(duration*rate)):
        t=i/rate
        edge=min(1,t/.02,(duration-t)/.1)
        if metal:
            value=sum(math.sin(2*math.pi*f*t)*math.exp(-t*(8+j*3)) for j,f in enumerate([91,337,619,1063]))*.13
            value+=(rng.random()*2-1)*math.exp(-30*t)*.15
        else:
            base=95+index*7
            phase=2*math.pi*(base*t+12*math.sin(t*5))
            pulses=max(0,math.sin(t*math.pi*7))**2 if laugh else .6+.4*math.sin(t*6)
            value=(math.sin(phase)*.25+math.sin(phase*2.03)*.12+math.sin(phase*3.9)*.055)*pulses
            value+=(rng.random()*2-1)*.02
        data.append(int(max(-.95,min(.95,value*edge))*32767))
    target=ASSETS/'sounds'/f'{name}.ogg';target.parent.mkdir(parents=True,exist_ok=True)
    with tempfile.TemporaryDirectory() as temp:
        wav=Path(temp)/'sound.wav'
        with wave.open(str(wav),'wb') as w:
            w.setnchannels(1);w.setsampwidth(2);w.setframerate(rate);w.writeframes(data.tobytes())
        subprocess.run(['ffmpeg','-hide_banner','-loglevel','error','-y','-i',str(wav),'-vn','-c:a','libvorbis','-q:a','4',str(target)],check=True)
print(f'Generated {len(names)} original sounds.')
