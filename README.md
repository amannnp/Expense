ExpenseClassifierApp
ExpenseClassifierApp is an Android application designed to automatically categorize user expenses. It uses machine learning to predict the category of each expense based on various features like amount, merchant name, and time of transaction. The app helps users manage their finances efficiently by providing insights into spending patterns and trends.
Features
Expense Input: Allows users to enter their expenses with details like amount, merchant, and category.
Category Prediction: Utilizes a machine learning model to predict the expense category based on features like amount, merchant, and time of day.
Firestore Integration: All user expenses are stored in Firebase Firestore for cloud-based data storage and real-time syncing.
Expense Visualization: Users can view their expenses in a structured format with a RecyclerView.
Real-time Updates: The app syncs with Firestore to display the most up-to-date expenses.
Simple and User-friendly Interface: Designed with ease of use in mind, providing an intuitive UI/UX.
Tech Stack
Kotlin: The programming language used to build the Android app.
Firebase Firestore: Used for real-time database management.
TensorFlow Lite: Utilized to integrate a machine learning model for expense categorization.
Android SDK: The development environment and libraries for building Android apps.
Installation
Clone the Repository: `git clone https://github.com/amannnp/Expense.git`
Open the Project in Android Studio: Open Android Studio and select 'Open an existing project'. Navigate to the cloned repository folder and select it.
Set Up Firebase: Create a Firebase project at https://console.firebase.google.com/. Follow the Firebase setup instructions to link your project with Firebase Firestore. Add your `google-services.json` to the app directory as instructed in the Firebase documentation.
Run the App: Build and run the project on an emulator or a physical device.
Usage
Adding/Deleting an Expense: Enter the expense amount, merchant name, and category (optional). The app will automatically predict the category using the machine learning model if not manually selected.
Viewing Expenses: Navigate to the main screen to view all recorded expenses in a RecyclerView.
Syncing Data: The app syncs your data with Firebase Firestore in real-time, ensuring your expenses are backed up and available across devices.
Machine Learning Model
The model is trained using features like amount, merchant name, and transaction time. The model is exported as a TensorFlow Lite file (`model.tflite`) and integrated into the Android app for real-time predictions.
Contributing
If you'd like to contribute to this project, feel free to open a pull request or submit an issue with your ideas or improvements.
Prepared by
This project was developed by Aman Pandey, a pre-final year B.Tech CSE student, 2026 batch.

