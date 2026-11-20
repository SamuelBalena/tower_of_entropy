// Existing Edge only; isolated profile, no dependency installation or public server.
const fs=require('fs'),path=require('path'),{pathToFileURL}=require('url');
const dir=path.resolve(__dirname,'../reports/m002_a2'),checks=[],errors=[];
const sleep=ms=>new Promise(r=>setTimeout(r,ms));
function check(test,pass,detail){checks.push({test,pass,detail});if(!pass)process.exitCode=1;}
(async()=>{
 const tabs=await(await fetch('http://127.0.0.1:9229/json/list')).json(),target=tabs.find(t=>t.type==='page');
 if(!target)throw Error('QA Edge page unavailable');
 const ws=new WebSocket(target.webSocketDebuggerUrl);await new Promise((r,j)=>{ws.onopen=r;ws.onerror=j;});
 let id=0;const pending=new Map();
 ws.onmessage=e=>{const m=JSON.parse(e.data);if(m.id){const p=pending.get(m.id);if(p){pending.delete(m.id);m.error?p.reject(Error(JSON.stringify(m.error))):p.resolve(m.result);}}else if(m.method==='Runtime.exceptionThrown')errors.push(m.params.exceptionDetails.text);};
 const call=(method,params={})=>new Promise((resolve,reject)=>{pending.set(++id,{resolve,reject});ws.send(JSON.stringify({id,method,params}));});
 const evaluate=async expression=>{const r=await call('Runtime.evaluate',{expression,returnByValue:true,awaitPromise:true});if(r.exceptionDetails)throw Error(JSON.stringify(r.exceptionDetails));return r.result.value;};
 await call('Runtime.enable');await call('Page.enable');await call('Emulation.setDeviceMetricsOverride',{width:1440,height:1000,deviceScaleFactor:1,mobile:false});
 await call('Page.navigate',{url:pathToFileURL(path.join(dir,'COMPARATOR.html')).href});await sleep(1000);
 const initial=await evaluate('({panels:document.querySelectorAll(".panel").length,images:[...document.images].map(i=>({width:i.naturalWidth,complete:i.complete})),markers:document.querySelectorAll(".markers button").length})');
 check('local_images_load',initial.panels===2&&initial.images.every(i=>i.complete&&i.width===854),initial);
 check('transition_markers',initial.markers>=6,initial.markers);
 await evaluate('time=0;document.querySelector("#speed").value="1";playing=true;previousClock=null;draw()');await sleep(650);const normal=await evaluate('time');await evaluate('playing=false');
 await evaluate('time=0;document.querySelector("#speed").value=".25";playing=true;previousClock=null;draw()');await sleep(650);const slow=await evaluate('time');await evaluate('playing=false;draw()');
 check('normal_and_slow_playback',normal>.4&&normal<1.2&&slow>.08&&slow<.3&&normal/slow>2.8&&normal/slow<5.3,{normal,slow,ratio:normal/slow});
 const stepped=await evaluate('time=1;document.querySelector("#next").click();time');check('frame_step',Math.abs(stepped-1.1)<.0001,stepped);
 await evaluate('panels[0].movement.value="walk";panels[0].movement.dispatchEvent(new Event("change"));panels[1].movement.value="sprint";panels[1].movement.dispatchEvent(new Event("change"));panels[1].angle.value="back";panels[1].angle.dispatchEvent(new Event("change"));time=1.2;draw()');await sleep(300);
 const selected=await evaluate('({left:document.images[0].getAttribute("src"),right:document.images[1].getAttribute("src"),loaded:[...document.images].every(i=>i.complete&&i.naturalWidth>0)})');
 check('movement_and_angle_selection',selected.left.includes('before/screenshots/walk/front/')&&selected.right.includes('after/screenshots/sprint/back/')&&selected.loaded,selected);
 await evaluate('panels.forEach(p=>{p.movement.value="speed_change";p.angle.value="side";markers(p)});time=2.4;draw()');await sleep(300);
 const png=await call('Page.captureScreenshot',{format:'png',captureBeyondViewport:false});fs.writeFileSync(path.join(dir,'COMPARATOR_PREVIEW.png'),Buffer.from(png.data,'base64'));
 check('no_browser_exceptions',errors.length===0,errors);
 fs.writeFileSync(path.join(dir,'COMPARATOR_TEST.json'),JSON.stringify({checks,errors,browser:'Existing Microsoft Edge headless, separate local QA profile'},null,2));
 console.log(checks);await call('Browser.close');ws.close();
})().catch(ex=>{console.error(ex);process.exitCode=1;});
