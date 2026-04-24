import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Input, OnChanges, Output, SimpleChanges } from '@angular/core';
import { FormsModule } from '@angular/forms';
import {
  JobPreference,
  UserProfile,
  createEmptyPreference,
  createEmptyProfile
} from '../../core/models/app.models';

@Component({
  selector: 'app-profile-page',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './profile-page.component.html',
  styleUrl: './profile-page.component.css'
})
export class ProfilePageComponent implements OnChanges {
  @Input() profile: UserProfile | null = null;
  @Input() busy = false;
  @Output() saveProfile = new EventEmitter<UserProfile>();
  @Output() resumeUpload = new EventEmitter<File>();

  draft: UserProfile = createEmptyProfile();

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['profile']) {
      this.draft = this.cloneProfile(this.profile ?? createEmptyProfile());
    }
  }

  addPreference(): void {
    this.draft.preferences = [...this.draft.preferences, createEmptyPreference()];
  }

  removePreference(index: number): void {
    this.draft.preferences = this.draft.preferences.filter((_, itemIndex) => itemIndex !== index);
  }

  submit(): void {
    this.saveProfile.emit(this.cloneProfile(this.draft));
  }

  handleResumeSelection(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    if (file) {
      this.resumeUpload.emit(file);
    }
    input.value = '';
  }

  trackByIndex(index: number): number {
    return index;
  }

  private cloneProfile(profile: UserProfile): UserProfile {
    return {
      ...profile,
      preferences: profile.preferences.map((item) => ({ ...item }))
    };
  }
}
