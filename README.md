# 🚀 LabourBook

**LabourBook** is an Android-based skilled-worker booking and workforce management platform designed to connect customers with workers such as plumbers, electricians, carpenters, and other service professionals.

The application combines a customer-facing service-booking experience with a dedicated worker dashboard and uses **Salesforce as the central backend/data platform**.

> 🚧 **Project Status:** Actively under development

---

## 📱 Project Overview

Finding a reliable skilled worker for a specific job can be difficult, while workers may not have a structured way to receive, manage, and schedule service requests.

LabourBook aims to provide a single platform where:

- Customers can discover workers based on service categories.
- Customers can select a supported service location.
- Customers can schedule work by date, start time, and duration.
- Workers can manage availability.
- Workers can receive pending job requests.
- Customers and workers can follow the booking lifecycle.

---

## ✨ Key Features

### 👤 Customer Side

- Customer registration and login
- Firebase Authentication
- Salesforce-backed customer management
- Location selection during onboarding
- Current-location detection using GPS
- Manual address selection
- Service-area validation
- Notification when a selected location is outside the supported service area
- Labour category browsing
- Worker discovery
- Worker profile and details
- Worker experience and skills
- Hourly and daily rates
- Worker availability
- Booking creation
- Booking details
- Booking cancellation
- Booking status tracking

### 👷 Worker Side

- Dedicated worker dashboard
- Dynamic worker profile information
- Labour category fetched from Salesforce
- Available / Busy status
- Availability synchronization with Salesforce
- Pending job requests
- Job description
- Work location
- Booking date/time
- Working hours
- Job amount
- Accept / Reject request workflow
- My Jobs section
- Schedule section
- Earnings section
- Worker profile
- Secure logout/session handling

---

# 📍 Location + Scheduling

A major part of LabourBook is the combination of **service location and scheduled work**.

When creating a booking, the customer can provide:

```text
Service Location
Preferred Date
Preferred Start Time
Working Duration
Booking Type
Work Description
```

Example:

```text
🔧 Pipe Repair

📍 Shastri Nagar, Dhanbad
📅 05 Sep 2026
🕙 Start: 10:00 AM
⏱ Duration: 2 Hours
💰 Amount: ₹700
```

### Planned Workflow

```text
Customer selects worker
        ↓
Selects service location
        ↓
Selects date and start time
        ↓
Selects working duration
        ↓
Creates booking
        ↓
Booking = Pending
        ↓
Worker receives request
        ↓
Worker checks schedule and location
        ↓
Worker accepts request
        ↓
Booking = Accepted
        ↓
Scheduled Work
        ↓
Work Completed
        ↓
Booking = Completed
```

---

# 📍 Location-Aware Work Start

A planned feature is a **Start Work** workflow.

```text
Worker reaches service location
        ↓
Worker taps "Start Work"
        ↓
App obtains worker GPS location
        ↓
Compare worker location with service location
        ↓
Worker is within allowed radius
        ↓
✅ Work Started
```

This can help verify that work is started from the customer's service location.

---

# ☁️ Salesforce Integration

Salesforce is used as the central backend/data platform.

The Android application communicates with Salesforce using REST APIs and SOQL queries.

## Worker Object

```text
Worker__c
```

Important fields:

| Field | API Name | Type |
|---|---|---|
| Worker Name | `Name` | Text |
| Address | `Address__c` | Long Text |
| Available | `Available__c` | Checkbox |
| Daily Rate | `Daily_Rate__c` | Currency |
| Email | `Email__c` | Email |
| Experience | `Experience__c` | Number |
| Hourly Rate | `Hourly_Rate__c` | Currency |
| Labour Category | `Labour_Category__c` | Lookup |
| Phone Number | `Phone_Number__c` | Phone |
| Profile URL | `Profile_URL__c` | URL |
| Skills | `Skills__c` | Long Text |

`Labour_Category__c` is a Salesforce Lookup relationship, allowing the application to retrieve the related labour category.

---

## Booking Object

```text
Booking__c
```

Important fields:

| Field | API Name | Type |
|---|---|---|
| Amount | `Amount__c` | Currency |
| Booking Date | `Booking_Date__c` | Date/Time |
| Booking Labour Status | `Booking_Labour_Status__c` | Picklist |
| Booking Type | `Booking_Type__c` | Picklist |
| Customer | `L_Customer__c` | Lookup |
| Work Address | `Work_Address__c` | Long Text |
| Work Description | `Work_Description__c` | Long Text |
| Worker | `Worker__c` | Lookup |
| Working Hour | `Working_Hour__c` | Number |

### Booking Types

```text
Hourly
Full Day (8 Hrs)
```

### Booking Statuses

```text
Pending
Accepted
Rejected
Completed
Cancelled
```

---

# 🏗️ Application Architecture

```text
                         LABOURBOOK
                             │
              ┌──────────────┴──────────────┐
              │                             │
          CUSTOMER                         WORKER
              │                             │
              ▼                             ▼
      Location Selection             Worker Dashboard
              │                             │
              ▼                             ▼
       Service Area Check             Availability
              │                             │
              ▼                             ▼
      Labour Categories              Pending Requests
              │                             │
              ▼                             ▼
       Worker Discovery              Accept / Reject
              │                             │
              └──────────────┬──────────────┘
                             │
                             ▼
                          BOOKING
                             │
                             ▼
                         SALESFORCE
                             │
              ┌──────────────┼──────────────┐
              ▼              ▼              ▼
           Worker         Customer       Booking
```

---

# 🛠️ Technology Stack

### Android
- Kotlin
- Android Studio
- XML layouts
- Material Design
- Fragments
- Navigation Component
- RecyclerView
- Kotlin Coroutines

### Backend / Cloud
- Salesforce
- Salesforce REST API
- SOQL
- Salesforce Custom Objects
- Salesforce Lookup Relationships

### Authentication
- Firebase Authentication
- Salesforce authentication/session handling

### Networking
- Retrofit
- REST APIs
- Coroutine-based asynchronous requests

### Location
- Android Location Services
- GPS/current location
- Service-area validation
- Location-aware work-start workflow

### Local Storage
- SharedPreferences

### Architecture
- Repository Pattern
- Retrofit API abstraction
- Asynchronous data loading
- Separation of UI and data-access logic

---

# 🔄 Booking Lifecycle

```text
                  ┌──────────┐
                  │ Pending  │
                  └────┬─────┘
                       │
              Worker accepts
                       │
                       ▼
                  ┌──────────┐
                  │ Accepted │
                  └────┬─────┘
                       │
                  Work starts
                       │
                       ▼
                  ┌──────────┐
                  │ In Work  │
                  └────┬─────┘
                       │
                Work completed
                       │
                       ▼
                  ┌──────────┐
                  │Completed │
                  └──────────┘

Pending ──────► Rejected
Pending ──────► Cancelled
```

---

# 🔐 Authentication & Session Handling

The application uses Firebase Authentication for user authentication and maintains Salesforce authentication information required for API communication.

Salesforce API requests use the authenticated access token:

```text
Authorization: Bearer <access_token>
```

The application handles invalid or expired Salesforce sessions and prevents unauthenticated API operations from being treated as successful.

---

# 📂 Project Structure

```text
LabourBook/
│
├── app/
│   └── src/
│       └── main/
│           ├── java/
│           │   └── com/example/labourbook/
│           │       ├── Fragment/
│           │       ├── model/
│           │       ├── network/
│           │       └── activities/
│           │
│           └── res/
│               ├── layout/
│               ├── drawable/
│               ├── navigation/
│               └── values/
│
└── README.md
```

---

# 🚧 Roadmap

### Implemented
- Customer booking workflow
- Location selection
- Service-area validation
- Worker profile retrieval
- Labour category retrieval through Salesforce Lookup
- Worker availability
- Worker dashboard
- Pending booking retrieval
- Booking information display

### Planned
- [ ] Connect Accept button to Salesforce
- [ ] Connect Reject button to Salesforce
- [ ] Worker My Jobs
- [ ] Worker schedule
- [ ] Real earnings calculation
- [ ] Complete booking lifecycle
- [ ] Customer booking history
- [ ] Push notifications
- [ ] Start Work functionality
- [ ] GPS-based work-location verification
- [ ] Improved map/location experience
- [ ] Production-level error handling
- [ ] Improved security and token management

---

# 🎯 Project Goals

1. Make it easier for customers to find skilled workers.
2. Give workers a structured way to manage service requests.
3. Support location-aware service bookings.
4. Allow customers to schedule work according to their requirements.
5. Maintain booking and worker data centrally through Salesforce.
6. Create a scalable foundation for a real-world service marketplace.

---

# 📚 Key Learning Outcomes

This project provides hands-on experience with:

- Android application development using Kotlin
- REST API integration
- Salesforce backend integration
- SOQL query development
- Salesforce Lookup relationships
- Firebase Authentication
- Retrofit networking
- Kotlin Coroutines
- Repository architecture
- Location services
- Service-area validation
- Booking management
- Worker availability management
- Role-based application workflows
- API error handling
- Real-world mobile application architecture

---

# 👨‍💻 Project

**LabourBook — Skilled Worker Booking & Workforce Management Platform**

Built as an end-to-end Android application with Salesforce integration, focusing on real-world customer booking, worker management, location, scheduling, and service execution workflows.

> ⭐ The project is continuously evolving as new modules and production-ready capabilities are added.
