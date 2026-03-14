import { Routes } from '@angular/router';
import { LoginComponent } from '../pages/login/login.component';
import { MetasVsConteoComponent } from '../pages/metas-vs-conteo/metas-vs-conteo.component';
import { MetasPorMunicipioComponent } from '../pages/metas-por-municipio/metas-por-municipio.component';
import { ConteoPorPuntoComponent } from '../pages/conteo-por-punto/conteo-por-punto.component';
export const routes: Routes = [
  { path: 'login', component: LoginComponent },
  { path: 'metas-vs-conteo', component: MetasVsConteoComponent },
  { path: 'metas-por-municipio', component: MetasPorMunicipioComponent },
  { path: 'conteo-por-punto', component: ConteoPorPuntoComponent },
  { path: '', redirectTo: 'metas-vs-conteo', pathMatch: 'full' },
  { path: '**', redirectTo: 'metas-vs-conteo' }
];