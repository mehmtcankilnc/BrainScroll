export const UN=-1;
export function lineClues(c){const o=[];let r=0;for(const f of c){if(f)r++;else if(r){o.push(r);r=0}}if(r)o.push(r);return o}
export function solveLine(known,clues){const n=known.length;let common=null;const line=Array(n).fill(0);
  const ok=(a,b,v)=>{for(let i=a;i<b;i++)if(known[i]!==UN&&known[i]!==v)return false;return true};
  function place(run,pos){
    if(run===clues.length){if(!ok(pos,n,0))return;for(let i=pos;i<n;i++)line[i]=0;if(!common)common=line.slice();else for(let i=0;i<n;i++)if(common[i]!==line[i])common[i]=UN;return}
    const len=clues[run];const rest=clues.slice(run+1).reduce((a,b)=>a+b,0)+(clues.length-run-1);
    for(let s=pos;s+len+rest<=n;s++){
      if(ok(pos,s,0)&&ok(s,s+len,1)){for(let i=pos;i<s;i++)line[i]=0;for(let i=s;i<s+len;i++)line[i]=1;
        if(run+1<clues.length){if(s+len<n&&ok(s+len,s+len+1,0)){line[s+len]=0;place(run+1,s+len+1)}}else place(run+1,s+len)}}
  }
  place(0,0);return common}
export function deduce(n,rowC,colC){const b=Array(n*n).fill(UN),order=[];let ch=true;
  while(ch){ch=false;for(let line=0;line<2*n;line++){const isRow=line<n,idx=isRow?line:line-n,cell=i=>isRow?idx*n+i:i*n+idx;
    const known=Array.from({length:n},(_,i)=>b[cell(i)]);const nx=solveLine(known,isRow?rowC[idx]:colC[idx]);if(!nx)return{order,ok:false};
    for(let i=0;i<n;i++)if(known[i]===UN&&nx[i]!==UN){b[cell(i)]=nx[i];order.push({cell:cell(i),v:nx[i],line,pass:0});ch=true}}}
  return{order,ok:order.length===n*n}}
