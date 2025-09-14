<div align="center"><h1> UTAR ACT - UTAR Event Listing and Management </h1></div>
<br><br>
<p align="center">
  <img src="https://github.com/user-attachments/assets/a075a738-e0ee-4ca4-872c-54d260e8735b" alt="icon_utaract_tranparent_crop" width="300" height="100" />
</p>
<br>

## Overview

UTAR ACT is a comprehensive Android application designed for managing university activities and events at Universiti Tunku Abdul Rahman (UTAR). The app facilitates event creation, management, and participation for both organizers and students (Guest).

<div align="center">

<table>
  <tr>
    <td align="center">
      <img src="https://github.com/user-attachments/assets/85d72903-42b3-4a36-8f07-94210286558a" width="240" height="520"/><br>Splash Screen
    </td>
    <td align="center">
      <img src="https://github.com/user-attachments/assets/c42dfded-95b9-4754-939c-cc1f34d83164" width="240" height="520"/><br>Event Listing
    </td>
    <td align="center">
      <img src="https://github.com/user-attachments/assets/13539571-af14-4385-8700-2cabd8864067" width="240" height="520"/><br>Event Detail
    </td>
  </tr>
  <tr>
    <td align="center">
      <img src="https://github.com/user-attachments/assets/ef63c5d5-29dd-446d-b34f-42cd7a2fbc3a" width="240" height="520"/><br>Registered Event History
    </td>
    <td align="center">
      <img src="https://github.com/user-attachments/assets/fed72944-fc2b-4a8d-8f76-f9edc6729ecb" width="240" height="520"/><br>Ticket System (For Verification)
    </td>
    <td align="center">
      <img src="https://github.com/user-attachments/assets/54cc4b03-0b45-48d3-a16d-3a164e5e8db9" width="240" height="520"/><br>Event Management
    </td>
  </tr>
    <tr>
    <td align="center">
      <img src="https://github.com/user-attachments/assets/a5a30f1e-7d7a-47e2-840c-b207e81556af" width="240" height="520"/><br>Event Applicant Management
    </td>
    <td align="center">
      <img src="https://github.com/user-attachments/assets/8b458095-5232-41df-ba96-1b4de04f7a1e" width="240" height="520"/><br>News Feed
    </td>
    <td align="center">
      <img src="https://github.com/user-attachments/assets/23e1b915-8c57-4526-8c46-a2b232822e1e" width="240" height="520"/><br>Chatbot
    </td>
  </tr>
</table>

</div>

## Features

### 🔐 Authentication & User Management
- **Firebase Auth** integration with anonymous sign-in
- **Guest Profile System** for user information management
- **Role-based Access** - Organizer and Participant roles
- **Profile Storage** with local JSON and cloud Firestore sync

### 📅 Event Management
- **Event Creation** with comprehensive forms including:
  - Event details (name, description, date, time, location)
  - Category selection and fee management
  - Maximum guest limits
  - Document upload with PDF text extraction
- **Event Discovery** with filtering and search capabilities
- **Event Applications** with approval workflow
- **QR Code Integration** for event check-ins
- **Ticket Generation** for confirmed events

### 🤖 AI-Powered Assistant
- **Gemini AI Integration** for intelligent event recommendations
- **Contextual Chat Support** for both general and event-specific queries
- **Smart Event Matching** based on user preferences
- **Document Analysis** for enhanced event information

### 📱 User Interface
- **Material Design 3** with modern UI components
- **Navigation Drawer** with intuitive menu structure
- **Swipe Refresh** functionality for real-time updates

### 📰 News & Notifications
- **News Feed** with creation and management capabilities
- **Push Notifications** via Firebase Cloud Messaging
- **Real-time Updates** for events and announcements
- **Notification History** tracking

### 🎫 Event Participation
- **Event Registration** with application status tracking
- **Joined Events** management and history
- **Application Status** (Pending, Confirmed, Rejected)
- **Event Details** with comprehensive information display

## Technical Architecture

### Backend Services
- **Firebase Firestore** - NoSQL database for data storage
- **Firebase Authentication** - User authentication and management
- **Firebase Storage** - File and document storage
- **Firebase Cloud Messaging** - Push notifications
- **Gemini AI API** - Intelligent assistant functionality

### Key Technologies
- **Android SDK 36** with minimum SDK 24
- **Java 11** for development
- **Material Design Components** for UI
- **View Binding** for type-safe view references
- **Navigation Component** for app navigation
- **Glide** for image loading and caching
- **ZXing** for QR code scanning
- **PDFBox** for document text extraction
- **OkHttp** for network requests


## Getting Started

### Prerequisites
- Android Studio
- Android SDK 24 or higher
- Firebase project setup

### Setup Instructions

1. **Clone the repository**
   ```bash
   git clone <repository-url>
   cd UTAR-ACT-a
   ```

2. **Firebase Configuration**
   - Create a Firebase project
   - Enable Authentication, Firestore, Storage, and Cloud Messaging
   - Download `google-services.json` and place it in the `app/` directory

3. **API Keys Configuration**
   - Add your Gemini AI API key to `local.properties`:
   ```properties
   GEMINI_API_KEY=your_gemini_api_key_here
   ```

4. **Build Configuration**
   - Set up signing configuration for release builds:
   ```properties
   MYAPP_RELEASE_STORE_FILE=path_to_keystore
   MYAPP_RELEASE_STORE_PASSWORD=keystore_password
   MYAPP_RELEASE_KEY_ALIAS=key_alias
   MYAPP_RELEASE_KEY_PASSWORD=key_password
   ```

## Configuration

### Firebase Setup
1. Enable Authentication with Anonymous sign-in
2. Create Firestore database with the following collections:
   - `events` - Event information
   - `guest_profiles` - User profiles
   - `applications` - Event applications
   - `news` - News articles
   - `notifications` - Push notifications

### Permissions
The app requires the following permissions:
- `INTERNET` - Network connectivity
- `POST_NOTIFICATIONS` - Push notifications
- `WRITE_EXTERNAL_STORAGE` - File operations (SDK ≤ 28)

## Usage

### For Organizers
1. **Create Events** - Use the event creation form to set up new activities
2. **Manage Applications** - Review and approve/reject participant applications
3. **Upload Documents** - Add PDF documents with automatic text extraction
4. **Generate QR Codes** - Create check-in codes for events
5. **News Management** - Create and manage news articles

### For Participants
1. **Browse Events** - Discover activities matching your interests
2. **Apply to Events** - Submit applications with your preferences
3. **Track Applications** - Monitor your application status
4. **Chat with AI** - Get personalized event recommendations
5. **Receive Notifications** - Stay updated on event changes

## Development

### Architecture Pattern
- **MVVM (Model-View-ViewModel)** architecture
- **Repository Pattern** for data management
- **Observer Pattern** with LiveData for reactive UI updates

### Key Components
- **Activities** - Main app screens and user interactions
- **Fragments** - Reusable UI components
- **ViewModels** - Business logic and data management
- **Adapters** - RecyclerView data binding
- **Utils** - Helper classes and data models

## Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add some amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## Version History

- **v1.0** - Initial release with core event management features
  - User authentication and profiles
  - Event creation and management
  - AI-powered assistant
  - News system
  - Push notifications
  - Ticket system
