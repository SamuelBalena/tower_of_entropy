const fs=require('fs'),path=require('path'),assert=require('assert/strict');
const dir=path.resolve(__dirname,'../reports/m002_a1'),r=JSON.parse(fs.readFileSync(path.join(dir,'RUNTIME.json'))),checks=[];
function check(name,pass,detail){checks.push({name,pass,detail});assert(pass,name+': '+JSON.stringify(detail));}
check('runtime_checks',r.checks.every(c=>c.pass),{passed:r.checks.filter(c=>c.pass).length,total:r.checks.length});
function starts(name){const samples=r.trace.filter(t=>t.case===name&&t.state.attacking&&t.state.started);return [...new Map(samples.map(t=>[t.state.startedTick,t.action.action])).values()];}
for(const [name,wanted]of Object.entries({combo:['LIGHT_ATTACK_A','LIGHT_ATTACK_B','LIGHT_ATTACK_C'],pause:['LIGHT_ATTACK_A','LIGHT_ATTACK_A'],light_then_heavy:['LIGHT_ATTACK_A','HEAVY_ATTACK'],heavy_then_light:['HEAVY_ATTACK','LIGHT_ATTACK_A']}))check(name+'_sequence',JSON.stringify(starts(name))===JSON.stringify(wanted),starts(name));
const queue=r.trace.filter(t=>t.case==='combo').map(t=>t.action.queued);check('queue_bounded',Math.max(...queue)===2&&queue.every(n=>n<=2),{maximum:Math.max(...queue)});
const expected={LIGHT_ATTACK_A:4,LIGHT_ATTACK_B:5,LIGHT_ATTACK_C:14,HEAVY_ATTACK:12};
for(const [action,delay]of Object.entries(expected)){const samples=r.trace.filter(t=>t.action.action===action&&t.state.impactTick>=t.state.startedTick&&t.state.startedTick>=0);check(action+'_impact_window',samples.length>0&&samples.every(t=>t.state.impactTick-t.state.startedTick===delay),{samples:samples.length,expectedTicks:delay});}
fs.writeFileSync(path.join(dir,'TIMELINE_AUDIT.json'),JSON.stringify({checks},null,2));console.log(checks);
