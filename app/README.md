# UTAR ACT - University Activity Management App

## Overview

UTAR ACT is a comprehensive Android application designed for managing university activities and events at Universiti Tunku Abdul Rahman (UTAR). The app facilitates event creation, management, and participation for both organizers and students.

## Features

### 🔐 Authentication & User Management
- **Firebase Authentication** integration with anonymous sign-in
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
- **Dark/Light Theme** support with automatic switching
- **Navigation Drawer** with intuitive menu structure
- **Responsive Layout** optimized for various screen sizes
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

### Project Structure
```
app/src/main/java/com/meow/utaract/
├── activities/           # Main application activities
│   ├── fragments/       # UI fragments
│   └── services/        # Service classes
├── adapters/            # RecyclerView adapters
├── firebase/           # Firebase service integrations
├── utils/              # Utility classes and data models
└── viewmodels/         # MVVM architecture view models
```

## Getting Started

### Prerequisites
- Android Studio Arctic Fox or later
- Android SDK 24 or higher
- Java 11
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

## Support

For support and questions, please contact the development team or create an issue in the repository.

## Version History

- **v1.0** - Initial release with core event management features
  - User authentication and profiles
  - Event creation and management
  - AI-powered assistant
  - News system
  - Push notifications
