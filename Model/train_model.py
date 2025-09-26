import pandas as pd
from sklearn.model_selection import train_test_split
from sklearn.ensemble import RandomForestClassifier
from sklearn.metrics import accuracy_score, classification_report
import joblib
import seaborn as sns
import matplotlib.pyplot as plt

def train_posture_model(csv_path='posture_dataset_final.csv'):
    """
    Loads the feature data from the CSV, trains a Random Forest model,
    evaluates its performance, and saves the trained model to a file.
    """
    # 1. Load the Dataset created by 'create_dataset.py'
    try:
        df = pd.read_csv(csv_path)
    except FileNotFoundError:
        print(f"Error: Dataset file '{csv_path}' not found.")
        print("Please make sure you have run 'create_dataset.py' first.")
        return

    print("--- Dataset loaded successfully ---")
    print(f"Found {len(df)} total samples.")
    print("Posture distribution in the dataset:\n", df['label'].value_counts())

    # 2. Prepare the Data for Training
    # X contains the features (the angles your model will learn from)
    X = df[['knee_angle', 'torso_hip_angle', 'neck_torso_angle']]
    # y contains the target labels (the correct posture name for each row)
    y = df['label']

    # Split data: 80% is used for training, 20% is held back for testing.
    # This lets us check if the model has actually learned or just memorized the data.
    X_train, X_test, y_train, y_test = train_test_split(
        X, y, test_size=0.2, random_state=42, stratify=y
    )
    # Stratify ensures the class distribution in train and test sets matches the original dataset.

    print(f"\nSplitting data: {len(X_train)} samples for training, {len(X_test)} samples for testing.")

    # 3. Build and Train the Random Forest Model
    print("\n--- Training the Random Forest model ---")
    # We create a "forest" of 500 decision trees. The model's prediction will be
    # the majority vote from all 500 trees.
    model = RandomForestClassifier(n_estimators=500, random_state=42)
    
    # This is the core learning step. The model finds patterns in the training data.
    model.fit(X_train, y_train)
    print("Model training complete.")

    # 4. Evaluate the Model's Performance
    print("\n--- Evaluating Model Performance on Unseen Test Data ---")
    y_pred = model.predict(X_test)
    accuracy = accuracy_score(y_test, y_pred)
    print(f"Model Accuracy: {accuracy * 100:.2f}%")
    
    print("\nDetailed Classification Report:")
    print(classification_report(y_test, y_pred, zero_division=0))
    
    # 5. Save the Trained Model
    # This saves the 'brain' of our operation to a single file.
    model_filename = 'posture_model_final.pkl'
    joblib.dump(model, model_filename)
    print(f"\n--- Model saved successfully to '{model_filename}' ---")
    print("You can now run 'run_live_model.py'.")

if __name__ == "__main__":
    train_posture_model()

