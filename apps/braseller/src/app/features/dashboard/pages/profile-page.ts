import { ChangeDetectionStrategy, Component } from '@angular/core';
import { UserProfilePage } from '../../user-profile/user-profile-page';

@Component({
  changeDetection: ChangeDetectionStrategy.OnPush,
  selector: 'app-dashboard-profile-page',
  standalone: true,
  imports: [UserProfilePage],
  template: `<app-user-profile-page></app-user-profile-page>`,
})
export class ProfilePage {}
