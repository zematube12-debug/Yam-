# YAM Rental (ያም ኪራይ)

Studio equipment rental, income/expense and monthly report app. Kotlin + Jetpack Compose + Room. English and Amharic.

## አገነባብ / Build
1. Create a GitHub repository and push this whole folder (include the `.github` folder).
2. Open the **Actions** tab, run **Build APK** (it also runs on every push).
3. When it finishes, open the run and download the **YAM-Rental-APK** artifact. Unzip it and install the `.apk` on the phone.

Every build is signed with the same key (`app/yam-rental.jks`), so a new APK installs over the old one and keeps all data.
Do not delete that file. If the repository is public, replace the key and passwords (see `app/build.gradle.kts`).

## Rules built in
- Rental is billed per 24h day: price per day x days.
- Returned late: up to 12 hours (changeable in Settings) is free. More than that adds one extra day, then one more extra day for every further 24 hours.
- A notification arrives when the return time comes, and again a few hours BEFORE every extra day is added.
- Deposit and payments are saved as income automatically and linked to the rental. Refunds are saved as expense.
- On the last evening of each month a "report is ready" notification opens the monthly report (shareable as PDF).
