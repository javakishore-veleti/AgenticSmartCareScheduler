import { Component } from '@angular/core';
import { RouterOutlet, RouterLink, RouterLinkActive } from '@angular/router';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet, RouterLink, RouterLinkActive],
  template: `
    <nav class="navbar navbar-expand-lg" style="background: linear-gradient(135deg, #0d9488 0%, #4f46e5 100%);">
      <div class="container-fluid">
        <a class="navbar-brand text-white fw-bold" routerLink="/">
          <i class="bi bi-shield-lock-fill me-2"></i>SmartCare Admin
        </a>
        <div class="collapse navbar-collapse">
          <ul class="navbar-nav ms-auto">
            <li class="nav-item"><a class="nav-link text-white" routerLink="/" routerLinkActive="active" [routerLinkActiveOptions]="{exact: true}"><i class="bi bi-speedometer2 me-1"></i>Dashboard</a></li>
            <li class="nav-item"><a class="nav-link text-white" routerLink="/agents" routerLinkActive="active"><i class="bi bi-cpu me-1"></i>Agents</a></li>
            <li class="nav-item"><a class="nav-link text-white" routerLink="/scenarios" routerLinkActive="active"><i class="bi bi-play-circle me-1"></i>Scenarios</a></li>
            <li class="nav-item"><a class="nav-link text-white" routerLink="/analytics" routerLinkActive="active"><i class="bi bi-graph-up me-1"></i>Analytics</a></li>
          </ul>
        </div>
      </div>
    </nav>

    <div class="container-fluid">
      <div class="row">
        <nav class="col-md-2 d-none d-md-block py-4" style="min-height: calc(100vh - 62px);">
          <div class="list-group list-group-flush">
            <a routerLink="/" routerLinkActive="active" [routerLinkActiveOptions]="{exact: true}" class="list-group-item list-group-item-action border-0 rounded-3 mb-1" style="background:transparent;">
              <i class="bi bi-speedometer2 me-2" style="color:#0d9488;"></i><span class="fw-semibold">Dashboard</span>
            </a>
            <a routerLink="/agents" routerLinkActive="active" class="list-group-item list-group-item-action border-0 rounded-3 mb-1" style="background:transparent;">
              <i class="bi bi-cpu me-2" style="color:#4f46e5;"></i><span class="fw-semibold">Agent Status</span>
            </a>
            <a routerLink="/scenarios" routerLinkActive="active" class="list-group-item list-group-item-action border-0 rounded-3 mb-1" style="background:transparent;">
              <i class="bi bi-play-circle me-2" style="color:#ea580c;"></i><span class="fw-semibold">Run Scenarios</span>
            </a>
            <a routerLink="/analytics" routerLinkActive="active" class="list-group-item list-group-item-action border-0 rounded-3 mb-1" style="background:transparent;">
              <i class="bi bi-graph-up me-2" style="color:#16a34a;"></i><span class="fw-semibold">Analytics</span>
            </a>
            <a routerLink="/datasets" routerLinkActive="active" class="list-group-item list-group-item-action border-0 rounded-3 mb-1" style="background:transparent;">
              <i class="bi bi-database me-2" style="color:#7c3aed;"></i><span class="fw-semibold">Datasets</span>
            </a>
            <a routerLink="/digital-twin" routerLinkActive="active" class="list-group-item list-group-item-action border-0 rounded-3 mb-1" style="background:transparent;">
              <i class="bi bi-diagram-3 me-2" style="color:#e11d48;"></i><span class="fw-semibold">Digital Twin</span>
            </a>
            <a routerLink="/audit" routerLinkActive="active" class="list-group-item list-group-item-action border-0 rounded-3 mb-1" style="background:transparent;">
              <i class="bi bi-journal-check me-2" style="color:#0d9488;"></i><span class="fw-semibold">Audit Log</span>
            </a>
            <hr class="my-3">
            <a routerLink="/settings" routerLinkActive="active" class="list-group-item list-group-item-action border-0 rounded-3 mb-1" style="background:transparent;">
              <i class="bi bi-gear me-2" style="color:#4f46e5;"></i><span class="fw-semibold">Settings</span>
            </a>
          </div>

          <div class="card mt-4 mx-2" style="background: linear-gradient(135deg, #0d9488 0%, #4f46e5 100%);">
            <div class="card-body text-white text-center">
              <i class="bi bi-activity fs-1"></i>
              <p class="mt-2 mb-0 small fw-bold">5 Agents Online</p>
              <p class="small opacity-75">All systems operational</p>
            </div>
          </div>
        </nav>

        <main class="col-md-10 ms-sm-auto px-4 py-4">
          <router-outlet></router-outlet>
        </main>
      </div>
    </div>
  `,
  styles: [`
    .list-group-item.active {
      background: linear-gradient(135deg, #0d948822 0%, #4f46e522 100%) !important;
      color: #0d9488;
      font-weight: 700;
    }
    .list-group-item:hover:not(.active) { background: #f0fdfa !important; }
  `]
})
export class AppComponent { title = 'SmartCare Admin Portal'; }
