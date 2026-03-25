import { Component } from '@angular/core';
import { RouterOutlet, RouterLink, RouterLinkActive } from '@angular/router';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet, RouterLink, RouterLinkActive],
  template: `
    <!-- Top Navbar -->
    <nav class="navbar navbar-expand-lg" style="background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);">
      <div class="container-fluid">
        <a class="navbar-brand text-white fw-bold" routerLink="/">
          <i class="bi bi-heart-pulse-fill me-2"></i>SmartCare
        </a>
        <button class="navbar-toggler" type="button" data-bs-toggle="collapse" data-bs-target="#navbarNav">
          <span class="navbar-toggler-icon"></span>
        </button>
        <div class="collapse navbar-collapse" id="navbarNav">
          <ul class="navbar-nav ms-auto">
            <li class="nav-item">
              <a class="nav-link text-white" routerLink="/" routerLinkActive="active" [routerLinkActiveOptions]="{exact: true}">
                <i class="bi bi-house-heart me-1"></i>Home
              </a>
            </li>
            <li class="nav-item">
              <a class="nav-link text-white" routerLink="/appointments" routerLinkActive="active">
                <i class="bi bi-calendar-check me-1"></i>My Appointments
              </a>
            </li>
            <li class="nav-item">
              <a class="nav-link text-white" routerLink="/reschedule" routerLinkActive="active">
                <i class="bi bi-arrow-repeat me-1"></i>Reschedule
              </a>
            </li>
            <li class="nav-item">
              <a class="nav-link text-white" routerLink="/preferences" routerLinkActive="active">
                <i class="bi bi-sliders me-1"></i>Preferences
              </a>
            </li>
          </ul>
          <div class="d-flex ms-3">
            <span class="badge rounded-pill text-bg-warning badge-pulse">
              <i class="bi bi-bell-fill me-1"></i>2 Upcoming
            </span>
          </div>
        </div>
      </div>
    </nav>

    <!-- Main Content -->
    <div class="container-fluid">
      <div class="row">
        <!-- Left Sidebar -->
        <nav class="col-md-2 d-none d-md-block sidebar py-4" style="min-height: calc(100vh - 62px);">
          <div class="list-group list-group-flush">
            <a routerLink="/" routerLinkActive="active" [routerLinkActiveOptions]="{exact: true}"
               class="list-group-item list-group-item-action border-0 rounded-3 mb-1"
               style="background: transparent;">
              <i class="bi bi-speedometer2 me-2" style="color: #667eea;"></i>
              <span class="fw-semibold">Dashboard</span>
            </a>
            <a routerLink="/appointments" routerLinkActive="active"
               class="list-group-item list-group-item-action border-0 rounded-3 mb-1"
               style="background: transparent;">
              <i class="bi bi-calendar2-week me-2" style="color: #198754;"></i>
              <span class="fw-semibold">Appointments</span>
            </a>
            <a routerLink="/reschedule" routerLinkActive="active"
               class="list-group-item list-group-item-action border-0 rounded-3 mb-1"
               style="background: transparent;">
              <i class="bi bi-arrow-clockwise me-2" style="color: #fd7e14;"></i>
              <span class="fw-semibold">Reschedule</span>
            </a>
            <a routerLink="/preferences" routerLinkActive="active"
               class="list-group-item list-group-item-action border-0 rounded-3 mb-1"
               style="background: transparent;">
              <i class="bi bi-gear me-2" style="color: #6f42c1;"></i>
              <span class="fw-semibold">Preferences</span>
            </a>
            <hr class="my-3">
            <a routerLink="/help" routerLinkActive="active"
               class="list-group-item list-group-item-action border-0 rounded-3 mb-1"
               style="background: transparent;">
              <i class="bi bi-question-circle me-2" style="color: #0dcaf0;"></i>
              <span class="fw-semibold">Help</span>
            </a>
          </div>

          <!-- Quick Status Card -->
          <div class="card mt-4 mx-2" style="background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);">
            <div class="card-body text-white text-center">
              <i class="bi bi-shield-check fs-1"></i>
              <p class="mt-2 mb-0 small fw-bold">SmartCare Active</p>
              <p class="small opacity-75">Context-aware scheduling</p>
            </div>
          </div>
        </nav>

        <!-- Content Area -->
        <main class="col-md-10 ms-sm-auto px-4 py-4">
          <router-outlet></router-outlet>
        </main>
      </div>
    </div>
  `,
  styles: [`
    .sidebar .list-group-item.active {
      background: linear-gradient(135deg, #667eea22 0%, #764ba222 100%) !important;
      color: #667eea;
      font-weight: 700;
    }
    .sidebar .list-group-item:hover:not(.active) {
      background: #f0f2ff !important;
    }
    .navbar .nav-link.active {
      border-bottom: 2px solid white;
    }
  `]
})
export class AppComponent {
  title = 'SmartCare Patient Portal';
}
