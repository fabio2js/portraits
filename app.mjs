import {styles,getStyle,filterStyles,wrapIndex,STORAGE_KEY,DRAFT_KEY,visualAsset,visualLabel,todayInRome,readProposal,proposalTiming,buildMessage,buildBrief} from './content.mjs';
const $=id=>document.getElementById(id);
const $$=selector=>Array.from(document.querySelectorAll(selector));
let selected='naturale',filter='all',modalIndex=0,modalItems=styles,opener=null,proposal=null;
try{selected=getStyle(localStorage.getItem(STORAGE_KEY)).id;}catch{$('preference-note').textContent='La scelta funziona per questa visita. Il browser non permette di ricordarla.';}
const asset=visualAsset;
function applyVisual(container,style){
  for(const name of ['synthetic-portrait','crop-0','crop-1','crop-2'])container.classList.remove(name);
  if(style.syntheticPanel!==undefined)container.classList.add('synthetic-portrait',`crop-${style.syntheticPanel}`);
  const label=container.querySelector('.image-label');if(label)label.textContent=visualLabel(style);
}
function renderGallery(){
  const list=filterStyles(filter);
  $('gallery').replaceChildren(...list.map(style=>{
    const button=document.createElement('button');button.type='button';button.className='gallery-card';button.dataset.style=style.id;
    button.setAttribute('aria-label',`Esplora ${style.name}, riferimento dimostrativo`);
    button.innerHTML=`<div class="gallery-picture"><img src="${asset(style)}" alt="${style.alt}" width="600" height="760" loading="lazy"><span class="image-label">${visualLabel(style)}</span><span class="gallery-arrow" aria-hidden="true">↗</span></div><div class="gallery-card-caption"><div><h3>${style.name}</h3><p>${style.subtitle}</p></div><span>${style.number}</span></div>`;
    applyVisual(button.querySelector('.gallery-picture'),style);
    button.addEventListener('click',()=>openGallery(style.id,button));return button;
  }));
  $('gallery-status').textContent=`${list.length} direzioni visive mostrate.`;
}
function renderOptions(){
  $('style-options').replaceChildren(...styles.map(style=>{
    const button=document.createElement('button');button.type='button';button.className='style-option';button.dataset.style=style.id;button.setAttribute('role','radio');button.setAttribute('aria-label',`${style.name}: ${style.subtitle}`);
    button.innerHTML=`<span class="style-option-number">${style.number}</span><div><h3>${style.name}</h3><p>${style.subtitle}</p></div><span class="style-check" aria-hidden="true">✓</span>`;
    button.addEventListener('click',()=>selectStyle(style.id));
    button.addEventListener('keydown',event=>{
      if(!['ArrowRight','ArrowDown','ArrowLeft','ArrowUp','Home','End'].includes(event.key))return;
      event.preventDefault();const index=styles.findIndex(s=>s.id===selected);
      const next=event.key==='Home'?0:event.key==='End'?styles.length-1:wrapIndex(index+(['ArrowRight','ArrowDown'].includes(event.key)?1:-1),styles.length);
      selectStyle(styles[next].id);$$('.style-option')[next].focus();
    });return button;
  }));
}
function selectStyle(id,announce=true){
  const style=getStyle(id);selected=style.id;
  $$('.style-option').forEach(b=>{const active=b.dataset.style===selected;b.setAttribute('aria-checked',String(active));b.tabIndex=active?0:-1;});
  $('style-image').src=asset(style);$('style-image').alt=style.alt;applyVisual($('style-image').parentElement,style);
  for(const [id,key] of [['style-detail-title','name'],['style-kicker','kicker'],['style-description','description'],['style-location','location'],['style-outfit','outfit'],['style-mood','mood'],['style-number','number']])$(id).textContent=style[key];
  $('style-palette').replaceChildren(...style.palette.map(color=>{const swatch=document.createElement('i');swatch.style.backgroundColor=color;swatch.setAttribute('aria-hidden','true');return swatch;}));
  $('style-palette').setAttribute('aria-label',`Palette indicativa di ${style.name}: ${style.palette.join(', ')}`);
  $('shoot-style').textContent=style.name;$('brief-style').textContent=style.name;
  if(proposal){proposal={...proposal,styleId:selected};$('message-text').value=buildMessage(proposal);$('message-status').textContent='Messaggio aggiornato con lo stile '+style.name+'.';}
  try{localStorage.setItem(STORAGE_KEY,selected);}catch{}
  if(announce)$('style-status').textContent=`${style.name} selezionato. La scheda di Capodimonte è stata aggiornata.`;
}
function renderModal(){
  const style=modalItems[modalIndex];
  $('dialog-image').src=asset(style);$('dialog-image').alt=style.alt;applyVisual($('dialog-image').parentElement,style);
  $('dialog-note').textContent=style.syntheticPanel!==undefined?'Immagine generata con AI, con una persona immaginaria. Non è uno scatto di Fabio né uno shooting reale.':'Esempio illustrato di direzione visiva. Non è un ritratto realizzato da Fabio.';
  $('dialog-title').textContent=style.name;$('dialog-description').textContent=style.description;
  $('dialog-index').textContent=`DIREZIONE ${style.number} / ESPLORAZIONE`;
  $('dialog-count').textContent=`${String(modalIndex+1).padStart(2,'0')} / ${String(modalItems.length).padStart(2,'0')}`;
  $('dialog-prev').disabled=modalItems.length<2;$('dialog-next').disabled=modalItems.length<2;
}
function openGallery(id,button){
  modalItems=filterStyles(filter);modalIndex=Math.max(0,modalItems.findIndex(s=>s.id===id));opener=button;renderModal();
  $('gallery-dialog').showModal();document.body.classList.add('dialog-open');$('dialog-close').focus();
}
function closeGallery(){$('gallery-dialog').close();}
$$('.filter').forEach(button=>button.addEventListener('click',()=>{
  filter=button.dataset.filter;$$('.filter').forEach(b=>{const active=b===button;b.classList.toggle('active',active);b.setAttribute('aria-pressed',String(active));});renderGallery();
}));
$('dialog-close').addEventListener('click',closeGallery);
$('gallery-dialog').addEventListener('close',()=>{document.body.classList.remove('dialog-open');if(opener?.isConnected)opener.focus();});
$('gallery-dialog').addEventListener('click',event=>{if(event.target===$('gallery-dialog')){const rect=$('gallery-dialog').getBoundingClientRect();if(event.clientX<rect.left||event.clientX>rect.right||event.clientY<rect.top||event.clientY>rect.bottom)closeGallery();}});
$('dialog-prev').addEventListener('click',()=>{modalIndex=wrapIndex(modalIndex-1,modalItems.length);renderModal();});
$('dialog-next').addEventListener('click',()=>{modalIndex=wrapIndex(modalIndex+1,modalItems.length);renderModal();});
$('gallery-dialog').addEventListener('keydown',event=>{if(event.key==='ArrowRight'||event.key==='ArrowLeft'){event.preventDefault();modalIndex=wrapIndex(modalIndex+(event.key==='ArrowRight'?1:-1),modalItems.length);renderModal();}});
$('dialog-select').addEventListener('click',()=>{const id=modalItems[modalIndex].id;closeGallery();selectStyle(id);$('stili').scrollIntoView({behavior:'smooth'});$$('.style-option').find(b=>b.dataset.style===id).focus({preventScroll:true});});
const menu=document.querySelector('.menu-toggle');
menu.addEventListener('click',()=>{const open=menu.getAttribute('aria-expanded')!=='true';menu.setAttribute('aria-expanded',String(open));menu.setAttribute('aria-label',open?'Chiudi il menu':'Apri il menu');$('nav').classList.toggle('is-open',open);});
$$('#nav a').forEach(a=>a.addEventListener('click',()=>{menu.setAttribute('aria-expanded','false');menu.setAttribute('aria-label','Apri il menu');$('nav').classList.remove('is-open');}));
document.addEventListener('keydown',event=>{if(event.key==='Escape'&&menu.getAttribute('aria-expanded')==='true'){menu.setAttribute('aria-expanded','false');$('nav').classList.remove('is-open');menu.setAttribute('aria-label','Apri il menu');menu.focus();}});
$('preferred-date').min=todayInRome();
function formValues(){return {personName:$('person-name').value,date:$('preferred-date').value,time:$('time-slot').value,notes:$('shoot-notes').value};}
function updateTiming(){
  try{const value=readProposal(formValues(),selected);$('preferred-date').setCustomValidity('');$('shoot-time').textContent=value.date||value.time!=='Da concordare'?proposalTiming(value):'Da concordare';}
  catch(error){$('preferred-date').setCustomValidity(error.message);$('shoot-time').textContent='Data da rivedere';}
}
['preferred-date','time-slot'].forEach(id=>$(id).addEventListener('change',updateTiming));
$('preferred-date').addEventListener('input',updateTiming);
$('brief-form').addEventListener('submit',event=>{
  event.preventDefault();$('preferred-date').min=todayInRome();
  try{proposal=readProposal(formValues(),selected);$('message-text').value=buildMessage(proposal);$('brief-form').hidden=true;$('message-panel').hidden=false;$('message-status').textContent='Proposta pronta. Nessun messaggio è stato inviato.';updateTiming();$('copy-message').focus();}
  catch(error){$('preferred-date').setCustomValidity(error.message);$('preferred-date').reportValidity();}
});
$('edit-message').addEventListener('click',()=>{proposal=null;$('message-panel').hidden=true;$('brief-form').hidden=false;$('person-name').focus();});
$('copy-message').addEventListener('click',async()=>{
  try{if(!navigator.clipboard?.writeText)throw new Error('Clipboard unavailable');await navigator.clipboard.writeText($('message-text').value);$('message-status').textContent='Messaggio copiato. Apri Instagram e incollalo nella conversazione.';}
  catch{$('message-text').focus();$('message-text').select();$('message-status').textContent='Copia automatica non disponibile. Il testo è selezionato: usa Ctrl+C oppure il comando Copia del dispositivo.';}
});
$('download-brief').addEventListener('click',()=>{
  if(!proposal)return;
  const blob=new Blob([buildBrief(proposal)],{type:'text/plain;charset=utf-8'});const url=URL.createObjectURL(blob);const link=document.createElement('a');link.href=url;link.download='Proposta_TF_Capodimonte.txt';document.body.append(link);link.click();link.remove();setTimeout(()=>URL.revokeObjectURL(url),1000);$('message-status').textContent='Scheda preparata per il download. La proposta resta da concordare.';
});
$('reset-brief').addEventListener('click',()=>{
  proposal=null;$('brief-form').reset();$('preferred-date').setCustomValidity('');$('message-text').value='';$('message-status').textContent='';$('message-panel').hidden=true;$('brief-form').hidden=false;selectStyle('naturale',false);
  try{localStorage.removeItem(STORAGE_KEY);localStorage.removeItem(DRAFT_KEY);}catch{}
  $('draft-status').textContent='Bozza cancellata da questo browser.';
  $('shoot-time').textContent='Da concordare';$('style-status').textContent='Proposta azzerata e preferenza rimossa dal browser.';$('person-name').focus();
});
function restoreDraft(){
  try{
    const raw=localStorage.getItem(DRAFT_KEY);if(!raw)return;
    const values=JSON.parse(raw);
    if(!values||typeof values!=='object'||typeof values.personName!=='string'||typeof values.notes!=='string'||typeof values.date!=='string')throw new Error('Invalid draft');
    const safe=readProposal({...values,date:''},values.styleId);
    $('person-name').value=safe.name;$('shoot-notes').value=safe.notes;$('time-slot').value=safe.time;
    $('preferred-date').value=/^\d{4}-\d{2}-\d{2}$/.test(values.date)?values.date:'';
    selected=safe.styleId;$('draft-status').textContent='Bozza ripristinata da questo browser. Puoi modificarla o azzerarla.';
  }catch{$('draft-status').textContent='Non è stato possibile ripristinare la bozza. Puoi compilare una nuova proposta.';}
}
$('save-draft').addEventListener('click',()=>{
  try{const values=formValues();readProposal(values,selected);localStorage.setItem(DRAFT_KEY,JSON.stringify({...values,styleId:selected}));$('draft-status').textContent='Bozza salvata solo in questo browser. Nessun messaggio è stato inviato.';}
  catch(error){$('draft-status').textContent=error.name==='QuotaExceededError'||error.name==='SecurityError'?'Il browser non permette di salvare la bozza. Puoi comunque copiare o scaricare la proposta.':error.message;}
});
restoreDraft();renderGallery();renderOptions();selectStyle(selected,false);updateTiming();
