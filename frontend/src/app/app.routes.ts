import { Routes } from '@angular/router';
import { LoginComponent } from '../pages/login/login.component';
import { ChangePasswordComponent } from '../pages/change-password/change-password.component';
import { MetasVsConteoComponent } from '../pages/metas-vs-conteo/metas-vs-conteo.component';
import { MetasPorMunicipioComponent } from '../pages/metas-por-municipio/metas-por-municipio.component';
import { ConteoPorPuntoComponent } from '../pages/conteo-por-punto/conteo-por-punto.component';
import { AdminUsersComponent } from '../pages/admin-users/admin-users.component';
import { AdminCreateUserComponent } from '../pages/admin-create-user/admin-create-user.component';
import { AdminMetasComponent } from '../pages/admin-metas/admin-metas.component';
import { authGuard } from '../guards/auth.guard';
import { adminGuard } from '../guards/admin.guard';
export const routes: Routes = [
  { path: 'login', component: LoginComponent },
  { path: 'change-password', component: ChangePasswordComponent, canActivate: [authGuard] },
  { path: 'metas-vs-conteo', component: MetasVsConteoComponent, canActivate: [authGuard] },
  { path: 'metas-por-municipio', component: MetasPorMunicipioComponent, canActivate: [authGuard] },
  { path: 'conteo-por-punto', component: ConteoPorPuntoComponent, canActivate: [authGuard] },
  { path: 'admin/users', component: AdminUsersComponent, canActivate: [authGuard, adminGuard] },
  { path: 'admin/create-user', component: AdminCreateUserComponent, canActivate: [authGuard, adminGuard] },
  { path: 'admin/metas', component: AdminMetasComponent, canActivate: [authGuard, adminGuard] },
  { path: '', redirectTo: 'metas-vs-conteo', pathMatch: 'full' },
  { path: '**', redirectTo: 'metas-vs-conteo' }
];
