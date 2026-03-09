# Project Specification: FlexFi

**Subtitle:** The "Strava for Finance" – Social Expense Tracking & Market Intelligence

**Summary Features:** Splitwise + Expense Tracker (bill ocr is a mode of expense input only can be manual entry with categories like splitwise way) + Flexing Financial Decisions and Expense managements (like Spotify wrapped or Strava Insta posts) + Bill , expense data can be cold stored for analysis of the market

## 1. Executive Summary

**FlexFi** is a native Android application designed to gamify financial responsibility. By combining **OCR (Optical Character Recognition)**, **group expense splitting**, and **social media integration**, it transforms the "chore" of budgeting into a social experience. The app operates on an "offline-first" model using phone numbers as unique identifiers, allowing users to track debts even for friends who haven't installed the app yet.

---

## 2. Problem Statement

- **The "Social Friction":** Most splitting apps require all parties to have an account, leading to abandoned groups.
    
- **Manual Entry Fatigue:** Users stop tracking expenses because typing in every line item is tedious.
    
- **Lack of Motivation:** Traditional finance apps are boring and private, offering no "reward" for good habits.
    
- **Data Silos:** Small-scale consumer spending data is often locked away, making it difficult for businesses to perform hyper-local market analysis.
    

---

## 3. Functional Requirements

### A. Expense Capture & OCR

- **Receipt Scanner:** Users can take a photo of a receipt. The app must extract the **Merchant Name**, **Date**, **Total Amount**, and **Individual Line Items** using Google ML Kit.
    
- **Manual Entry:** Fallback for cash transactions or digital receipts.
    
- **Categorization:** Auto-assign categories (Food, Transport, etc.) using simple keyword matching or NLP.
    

### B. "Ghost" Group Splitting

- **Phone-Number ID:** Users can add anyone from their contact list to a group using a phone number.
    
- **Identity Merging:** If a person is added via two different numbers, the app provides a "Merge Contact" feature to consolidate their debt.
    
- **Settlement Tracking:** A simple dashboard showing who owes whom, with the ability to send "Remind via WhatsApp" prompts.
    

### C. The "Flex" (Social Engine)

- **Financial Fitness Score:** A proprietary algorithm that ranks users based on budget adherence and "smart" spending.
    
- **Flex-Cards:** Generate a visually appealing graphic (JPG/PNG) of the user's weekly "Wrapped" or "Monthly Milestone" for Instagram/Snapchat.
    
- **Streaks:** Tracking consecutive days of scanning receipts or staying under budget.
    

### D. Data Warehouse Engine (B2B)

- **Anonymized Sync:** Syncing receipt data (minus personal identifiers) to a central database.
    
- **Market Mapping:** Tagging expenses with GPS coordinates (if permitted) to track regional brand popularity.
    

---

## 4. Technical Requirements (The Tech Stack)

|**Component**|**Technology**|
|---|---|
|**Language**|Kotlin|
|**UI Framework**|Jetpack Compose (Modern, declarative UI)|
|**Local Database**|Room Persistence Library|
|**Image Processing**|CameraX + Google ML Kit (Text Recognition)|
|**Background Tasks**|WorkManager (For syncing data and notifications)|
|**Analytics/Cloud**|Firebase Auth & Firestore (For group sync)|
|**Graphics**|PixelCopy API / Canvas (For Social Card generation)|

---

## 5. System Architecture

### Data Entity Relationship (Simplified)

- **User/Contact:** Name, List of Phone Numbers, Balance.
    
- **Transaction:** Amount, Category, Timestamp, Receipt Image Path, "SplitType" (Equal, Percentage, Exact).
    
- **Group:** List of Member IDs, Total Spend.
    

---

## 6. Revenue Model

1. **B2C (Freemium):**
    
    - _Free:_ Unlimited scans, basic splitting, social sharing.
        
    - _Premium:_ Advanced analytics, PDF export for taxes, and ad-free experience.
        
2. **B2B (Data-as-a-Service):**
    
    - Selling anonymized, aggregated consumer trend reports to retail brands and market researchers.
        
3. **Affiliate:**
    
    - Contextual "Smart Coupons" based on scanned receipt history.
        

---

## 7. Roadmap & Milestones

- **Phase 1 (MVP):** Local Room DB setup, CameraX integration, and basic manual expense adding.
    
- **Phase 2 (The Split):** Phone-number-based group logic and debt calculation engine.
    
- **Phase 3 (The Flex):** ML Kit OCR integration and "Social Card" generator.
    
- **Phase 4 (The Startup):** Firebase backend for real-time group syncing and anonymized data pipeline.
    

---

## ABOUT
---

### **FlexFi: The Social Financial Intelligence Ecosystem**

**FlexFi** is an AI-driven mobile platform that transforms the traditional utility of expense tracking into a social-first "financial fitness" experience. Designed for the modern consumer, the app automates the capture of financial data through native Android OCR (Optical Character Recognition) technology and introduces a frictionless social layer for group debt management.

#### **Core Features & Functionality**

- **Intelligent Receipt Extraction:** Utilizing high-speed OCR, FlexFi converts physical paper receipts into structured digital line items, automatically identifying merchants, dates, and category-specific spending without manual entry.
    
- **Zero-Barrier Group Splitting:** A unique "Ghost Profile" system allows users to manage group expenses using only phone numbers. This removes the friction of requiring all participants to install the app, enabling a single user to maintain a complete ledger for their entire social circle.
    
- **Identity Consolidation:** The app utilizes a sophisticated contact-merging engine to link multiple phone numbers to a single individual, ensuring debt accuracy across different communication platforms.
    
- **"Financial Flex" Social Integration:** Inspired by fitness tracking apps, FlexFi generates shareable "Financial Wrapped" cards. These data visualizations allow users to showcase healthy spending habits, budget streaks, and "Thrift Scores" on social media platforms like Instagram and Snapchat.
    
- **Privacy-First Data Architecture:** While providing users with personal insights, the platform operates an anonymized data engine that aggregates consumer trends. This creates a high-value B2B market intelligence warehouse for real-time retail analysis without compromising individual user privacy.
    

#### **The Innovation**

FlexFi bridges the gap between private financial management and social status. By replacing the anxiety of budgeting with the "social proof" of financial health, it incentivizes consistent tracking—turning raw consumer data into both a personal asset for the user and a strategic asset for the global market.

---