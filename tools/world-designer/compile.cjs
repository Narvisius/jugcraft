// Headless equivalent of Export world pack; no network or third-party Node dependencies.
const fs=require('node:fs');
const M=require('./model.js');
const [designFile,catalogFile,outputFile]=process.argv.slice(2);
if(!designFile||!catalogFile||!outputFile){console.error('Usage: node compile.cjs design.jugcraft.json catalog.json output.zip');process.exitCode=1;}
else{try{const design=JSON.parse(fs.readFileSync(designFile,'utf8')),catalog=JSON.parse(fs.readFileSync(catalogFile,'utf8'));fs.writeFileSync(outputFile,M.zip(M.packFiles(design,catalog)),{flag:'wx'});console.log(`Created ${outputFile}`);}catch(error){console.error(error.message);process.exitCode=1;}}
