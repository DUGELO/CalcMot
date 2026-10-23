import { readFileSync, mkdirSync, writeFileSync } from 'node:fs';
import Ajv from 'ajv/dist/2020.js';
import addFormats from 'ajv-formats';
import standalone from 'ajv/dist/standalone/index.js';
import codegen from 'ajv/dist/compile/codegen/index.js';

const ajv = new Ajv({ strict: false, code: { source: true, esm: true, formats: codegen._`formats` } });
addFormats(ajv);
const schema = JSON.parse(readFileSync('../docs/contracts/entitlement.schema.json', 'utf8'));
ajv.addSchema(schema, 'entitlement');
ajv.addSchema({ $id: 'legacyGrant', ...schema.$defs.LegacyGrant });
ajv.addSchema({ $id: 'legacyClaim', ...schema.$defs.LegacyClaim });
let code = standalone(ajv, { validateLegacyGrant: 'legacyGrant', validateLegacyClaim: 'legacyClaim' });
code = code.replaceAll('require("ajv/dist/runtime/ucs2length").default', 'ucs2length')
  .replaceAll("require('ajv/dist/runtime/ucs2length').default", 'ucs2length');
const source = 'import formatModule from "ajv-formats/dist/formats.js";\n' +
  'import ucs2Module from "ajv/dist/runtime/ucs2length.js";\n' +
  'const formats = formatModule.fullFormats;\n' +
  'const ucs2length = ucs2Module.default?.default || ucs2Module.default || ucs2Module;\n' + code;
mkdirSync('src/generated', { recursive: true });
writeFileSync('src/generated/legacy-validator.js', source);
