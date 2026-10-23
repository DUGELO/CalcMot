import { readFileSync, mkdirSync, writeFileSync } from 'node:fs';
import Ajv from 'ajv/dist/2020.js';
import addFormats from 'ajv-formats';
import standalone from 'ajv/dist/standalone/index.js';
import codegen from 'ajv/dist/compile/codegen/index.js';

// Compile at build time: Workers forbid runtime eval/new Function.
const ajv = new Ajv({ strict: false, code: { source: true, esm: true, formats: codegen._`formats` } });
addFormats(ajv);
const schema = JSON.parse(readFileSync('../docs/contracts/entitlement.schema.json', 'utf8'));
const validate = ajv.compile(schema);
const source = 'import formatModule from "ajv-formats/dist/formats.js";\nconst formats = formatModule.fullFormats;\n' + standalone(ajv, validate);
mkdirSync('src/generated', {recursive: true});
writeFileSync('src/generated/entitlement-validator.js', source);
