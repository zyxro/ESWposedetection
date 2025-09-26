# Data Preprocessing

The `create_dataset.py` script is responsible for preprocessing pose data captured in JSON format. The overall approach involves:

1. **Feature Extraction:**
   - Extracts key right-side keypoints (ear, shoulder, hip, knee, ankle) from the JSON files.
   - For each keypoint, retrieves its x, y coordinates and confidence score.
   - Calculates three key angles:
     - **Knee Angle:** The angle formed by the ankle, knee, and hip. This captures the bend at the knee joint.
     - **Torso-Hip Angle:** The angle formed by the knee, hip, and shoulder. This measures the alignment between the leg and torso.
     - **Neck-Torso Angle:** The angle formed by the hip, shoulder, and ear. This captures the posture of the upper body.
   - Note: Confidence filtering is currently disabled, but can be re-enabled for higher data quality.

2. **Confidence Filtering:**
   - Ensures that only keypoints with a confidence score above a threshold (0.3) are used.

3. **Dataset Creation:**
   - Processes JSON files from labeled posture directories.
   - Combines extracted features with posture labels to create a structured dataset.

The resulting dataset is saved as a CSV file (`posture_dataset.csv`) for further analysis or machine learning tasks.

# Model Training

The `train_model.py` script is responsible for training a machine learning model to classify different postures based on the processed dataset. The process involves:

1. **Data Splitting:**
   - The dataset is split into training and testing sets, ensuring the class distribution is preserved using stratification.

2. **Model Selection:**
   - A Random Forest Classifier is used as the model for training.

3. **Training:**
   - The model is trained on the training set using the extracted features.

4. **Evaluation:**
   - The trained model is evaluated on the testing set to measure its accuracy and performance.

# Training and Evaluation

The `train_model.py` script trains a Random Forest Classifier to classify postures based on the processed dataset. Below are the key steps and outputs:

1. **Training the Model:**
   - The script uses a Random Forest Classifier with 500 decision trees (`n_estimators=500`).
   - The model is trained on the training dataset to learn patterns and relationships between features and labels.

2. **Classification Report:**
   - After training, the model is evaluated on the test dataset.
   - The `classification_report` provides detailed metrics, including:
     - **Precision:** The proportion of true positive predictions among all positive predictions.
     - **Recall:** The proportion of true positive predictions among all actual positives.
     - **F1-Score:** The harmonic mean of precision and recall, balancing both metrics.
     - **Support:** The number of occurrences of each class in the test dataset.

3. **Model Accuracy:**
   - The overall accuracy of the model is displayed as a percentage, indicating the proportion of correct predictions.

4. **Saving the Model:**
   - The trained model is saved as `posture_model_final.pkl` for future use in live posture detection.

These outputs help in understanding the model's performance and readiness for deployment.

---

## Setup: Python dependencies (Windows PowerShell)

Use Python 3.10–3.11 and a virtual environment:

1) Create and activate venv

```powershell
python -m venv .venv
.\.venv\Scripts\Activate.ps1
```

2) Upgrade pip and install requirements

```powershell
python -m pip install --upgrade pip
pip install -r requirements.txt
```

If you have a CUDA GPU and want acceleration, install matching torch/torchvision builds from https://pytorch.org/get-started/locally/ before installing the rest.

## Run live YOLO pose capture

The `run_live_model.py` (or the example webcam script you shared) captures a frame, runs YOLO pose, and saves keypoints JSON under `webcam_captures/`.

```powershell
.\.venv\Scripts\Activate.ps1
python run_live_model.py
```

Tips:
- Adjust the camera index in `cv2.VideoCapture(2)` to 0/1/2 depending on your system.
- First run may download weights; ensure internet access.
- If `python` opens Microsoft Store, disable App Execution Aliases for Python and reopen PowerShell.