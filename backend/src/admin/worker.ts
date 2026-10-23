import {WorkerEntrypoint} from 'cloudflare:workers';
import {handleAdmin,type AdminEnv} from './api';
import {failure,environmentResponse} from './security';

/** Only RPC is usable, even if an HTTP route is accidentally added later. */
export default class S06AdminWorker extends WorkerEntrypoint<AdminEnv> {
  override async fetch(_request:Request) {return environmentResponse(failure('FORBIDDEN',403),this.env)}
  async handle(request:Request):Promise<Response> {return handleAdmin(request,this.env)}
}
