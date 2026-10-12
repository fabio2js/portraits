export const INSTAGRAM = 'https://www.instagram.com/fmarano_photos/';
export const STORAGE_KEY = 'fmarano.ritratto.stile.v1';
export const styles = [
  { id:'naturale', number:'01', name:'Naturale', subtitle:'Luce morbida, gesti spontanei', group:'naturale', kicker:'LUCE NATURALE / SPONTANEITÀ', description:"Un ritratto semplice e vicino alla persona. Espressioni spontanee, piccoli gesti e luce morbida: possiamo partire da una conversazione e lasciare che le pose arrivino con calma.", location:"Cercare un tratto in ombra aperta e uno sfondo tranquillo, da scegliere sul posto.", outfit:"Toni neutri, tessuti semplici e un abito in cui ti senti a tuo agio.", mood:'Morbido · essenziale · vicino', palette:['#5b6d43','#a4ab7f','#d3c9ad'], alt:'Illustrazione segnaposto di una figura in toni verdi e crema, per una direzione di ritratto naturale.' },
  { id:'ambientato', number:'02', name:'Ambientato', subtitle:'La persona dentro il luogo', group:'naturale', kicker:'PERSONA / PAESAGGIO', description:"Il luogo entra nel racconto. Un'inquadratura più ampia mette in dialogo la persona, i percorsi e il verde. Un passo, una pausa, un dettaglio: il ritratto può nascere anche dal movimento.", location:"Un percorso o un angolo del verde, mantenendo l'ambiente leggibile nell'inquadratura.", outfit:"Un colore che dialoghi con il paesaggio, scarpe comode per spostarsi.", mood:'Aperto · narrativo · in movimento', palette:['#738468','#b2b096','#ded8c2'], alt:'Illustrazione segnaposto di una figura piccola in un paesaggio alberato, per un ritratto ambientato.' },
  { id:'cinematico', number:'03', name:'Cinematico', subtitle:'Ombre, atmosfera, una scena', group:'atmosfera', kicker:'ATMOSFERA / NARRAZIONE', description:"Un'immagine che suggerisce una scena e lascia qualcosa fuori dal quadro. Ombre, colore e uno sguardo altrove possono dare al ritratto un tono raccolto, quasi cinematografico.", location:"Un punto con luce e ombra ben separate, valutando l'atmosfera disponibile quel giorno.", outfit:"Una palette contenuta; una giacca o un dettaglio di colore come accento.", mood:'Intimo · sospeso · narrativo', palette:['#1e3d38','#8b694b','#bda97d'], alt:'Illustrazione segnaposto di un volto con ombre scure e luce calda, per una direzione cinematica.' },
  { id:'editoriale', number:'04', name:'Editoriale', subtitle:'Abito, posa e composizione', group:'atmosfera', kicker:'STILE / COMPOSIZIONE', description:"L'abbigliamento e la posa diventano parte dell'idea. Linee più definite, un gesto intenzionale e una composizione pensata insieme: un approccio con spazio per sperimentare.", location:"Uno sfondo semplice dove forma, abito e postura possano emergere.", outfit:"Un capo protagonista o un accessorio; meglio concordare la scelta prima.", mood:'Deciso · grafico · costruito', palette:['#a55137','#d2ad85','#393c30'], alt:'Illustrazione segnaposto di una figura con abito strutturato in toni terracotta, per una direzione editoriale.' },
  { id:'fine-art', number:'05', name:'Fine art', subtitle:"Un'idea, più che una posa", group:'atmosfera', kicker:'FORMA / SUGGESTIONE', description:"Un'esplorazione più interpretativa. Silhouette, dettagli e spazio vuoto possono tradurre un'emozione in immagine. Partiamo da un'idea condivisa, senza bisogno di raccontare tutto.", location:"Un angolo con pochi elementi e forme interessanti, da individuare durante l'incontro.", outfit:"Forme semplici e una palette ridotta, al servizio dell'idea.", mood:'Poetico · essenziale · evocativo', palette:['#cdc5ab','#8c9580','#515c4c'], alt:'Illustrazione segnaposto di una silhouette astratta e una grande forma circolare, per una direzione fine art.' },
  { id:'bianco-nero', number:'06', name:'Bianco e nero', subtitle:'Espressione, luce e carattere', group:'bn', kicker:'ESPRESSIONE / CONTRASTO', description:"Il colore lascia spazio allo sguardo, alle forme e alle sfumature della luce. Possiamo cercare un'espressione raccolta o un gesto deciso, pensando già allo scatto in bianco e nero.", location:"Uno sfondo pulito e una luce che disegni il volto senza distrarre.", outfit:"Tessuti e forme leggibili, evitando motivi troppo fitti.", mood:'Diretto · senza tempo · essenziale', palette:['#32352f','#96998e','#dfdfd3'], alt:'Illustrazione segnaposto di un volto in scala di grigi, per una direzione di ritratto in bianco e nero.' }
];
const syntheticPortraits={naturale:0,editoriale:1,'bianco-nero':2};
for(const style of styles){
  if(style.id in syntheticPortraits){
    style.syntheticPanel=syntheticPortraits[style.id];
    style.alt=`Ritratto sintetico generato con AI, riferimento dimostrativo per ${style.name}; non è una fotografia di Fabio.`;
  }
}
export const DRAFT_KEY='fmarano.ritratto.bozza.v1';
export function visualAsset(style){return style.syntheticPanel!==undefined?'./assets/portrait-studies.png':`./assets/${style.id}.svg`;}
export function visualLabel(style){return style.syntheticPanel!==undefined?'ESEMPIO SINTETICO · AI':'SEGNAPOSTO ILLUSTRATO';}
export function getStyle(id){return styles.find(s=>s.id===id)||styles[0];}
export function filterStyles(group){return group==='all'?styles:styles.filter(s=>s.group===group);}
export function wrapIndex(index,length){return length ? ((index%length)+length)%length : 0;}
export function todayInRome(now=new Date()){
  const parts=new Intl.DateTimeFormat('en-CA',{timeZone:'Europe/Rome',year:'numeric',month:'2-digit',day:'2-digit'}).formatToParts(now);
  const part=name=>parts.find(p=>p.type===name).value;
  return `${part('year')}-${part('month')}-${part('day')}`;
}
export function isValidDate(value){
  if(!/^\d{4}-\d{2}-\d{2}$/.test(value))return false;
  const d=new Date(value+'T12:00:00Z');
  return !Number.isNaN(d.getTime())&&d.toISOString().slice(0,10)===value;
}
export function formatDate(value){
  return isValidDate(value)?new Intl.DateTimeFormat('it-IT',{dateStyle:'long',timeZone:'UTC'}).format(new Date(value+'T12:00:00Z')):'Da concordare';
}
export function readProposal(values,styleId,today=todayInRome()){
  const name=String(values.personName||'').trim();
  const notes=String(values.notes||'').trim();
  const date=String(values.date||'');
  const time=['Mattina','Pomeriggio'].includes(values.time)?values.time:'Da concordare';
  if(name.length>60)throw new Error('Il nome può avere al massimo 60 caratteri.');
  if(notes.length>700)throw new Error('L’idea può avere al massimo 700 caratteri.');
  if(date&&(!isValidDate(date)||date<today))throw new Error('Scegli una data valida da oggi in poi, oppure lascia il campo vuoto.');
  return {name,notes,date,time,styleId:getStyle(styleId).id};
}
export function proposalTiming(proposal){
  const date=proposal.date?formatDate(proposal.date):'Data da concordare';
  return proposal.time==='Da concordare'?date:`${date} · ${proposal.time.toLowerCase()}`;
}
export function buildMessage(proposal){
  const style=getStyle(proposal.styleId);
  return [`Ciao Fabio${proposal.name?`, sono ${proposal.name}`:''}!`,
    `Mi piacerebbe partecipare a uno shooting TF all'aperto al Bosco di Capodimonte, a Napoli.`,
    `La direzione che preferisco è: ${style.name}.`,
    `Disponibilità proposta: ${proposalTiming(proposal)}.`,
    proposal.notes?`La mia idea: ${proposal.notes}`:'',
    `Possiamo concordare punto d'incontro, durata, abbigliamento, eventuali spese e numero e tempi di consegna delle foto, insieme ai permessi di utilizzo e pubblicazione?`
  ].filter(Boolean).join('\n\n');
}
export function buildBrief(proposal){
  return ['PROPOSTA SHOOTING TF — FABIO MARANO','Stato: proposta da concordare; nessuna prenotazione effettuata.','Luogo: Bosco di Capodimonte, Napoli',`Stile: ${getStyle(proposal.styleId).name}`,`Disponibilità proposta: ${proposalTiming(proposal)}`,'Punto d’incontro, durata, fotografie e utilizzi: da concordare.','Accesso, orari, eventuali permessi e meteo: da verificare prima della sessione.','Profilo: '+INSTAGRAM,'','MESSAGGIO',buildMessage(proposal)].join('\n');
}
