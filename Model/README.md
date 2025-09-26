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