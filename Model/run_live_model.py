import os
import time
import glob
import json
import numpy as np
import joblib
import pandas as pd # <-- Make sure to import pandas

# --- Helper function to calculate angles (from create_dataset.py) ---
def calculate_angle(a, b, c):
    """Calculates the angle between three points in 2D space."""
    a, b, c = np.array(a), np.array(b), np.array(c)
    ba, bc = a - b, c - b
    dot_product = np.dot(ba, bc)
    magnitude_ba, magnitude_bc = np.linalg.norm(ba), np.linalg.norm(bc)
    cosine_angle = np.clip(dot_product / (magnitude_ba * magnitude_bc + 1e-6), -1.0, 1.0)
    return np.degrees(np.arccos(cosine_angle))

def process_keypoint_file(filepath, model, feature_names):
    """
    Loads a single JSON file, extracts features, and returns the model's prediction.
    """
    try:
        with open(filepath, 'r') as f:
            data = json.load(f)
    except Exception as e:
        return f"Error reading file: {e}"

    if not data.get('persons'):
        return "No persons detected in file."

    keypoints = {part['part_name']: part for part in data['persons'][0]['keypoints']}
    
    required_keypoints = {
        'ear': keypoints.get('right_ear'), 'shoulder': keypoints.get('right_shoulder'),
        'hip': keypoints.get('right_hip'), 'knee': keypoints.get('right_knee'),
        'ankle': keypoints.get('right_ankle')
    }

    if not all(required_keypoints.values()):
        missing = [k for k, v in required_keypoints.items() if v is None]
        return f"Skipping file: Missing keypoint(s): {', '.join(missing)}"

    ear_coords = [required_keypoints['ear']['x'], required_keypoints['ear']['y']]
    shoulder_coords = [required_keypoints['shoulder']['x'], required_keypoints['shoulder']['y']]
    hip_coords = [required_keypoints['hip']['x'], required_keypoints['hip']['y']]
    knee_coords = [required_keypoints['knee']['x'], required_keypoints['knee']['y']]
    ankle_coords = [required_keypoints['ankle']['x'], required_keypoints['ankle']['y']]

    knee_angle = calculate_angle(ankle_coords, knee_coords, hip_coords)
    torso_hip_angle = calculate_angle(knee_coords, hip_coords, shoulder_coords)
    neck_torso_angle = calculate_angle(hip_coords, shoulder_coords, ear_coords)

    # --- THIS IS THE FIX ---
    # Instead of a simple list, we create a pandas DataFrame with the exact
    # column names the model was trained on.
    live_features_df = pd.DataFrame(
        [[knee_angle, torso_hip_angle, neck_torso_angle]], 
        columns=feature_names
    )
    
    # Now we predict using the DataFrame, and the warning will disappear.
    prediction = model.predict(live_features_df)[0]
    # ----------------------
    
    return f"Posture: {prediction.upper()}"

# --- Main Application ---
def run_folder_watcher(watch_folder='keypoints_to_analyze'):
    """
    Continuously watches a folder for new .json files and analyzes them.
    """
    model_filename = 'posture_model_final.pkl'
    try:
        model = joblib.load(model_filename)
        # We can get the feature names the model was trained on directly from the model itself
        feature_names = model.feature_names_in_
        print(f"Model '{model_filename}' loaded successfully.")
        print(f"Model expects features: {feature_names}")
    except FileNotFoundError:
        print(f"Error: Model file '{model_filename}' not found.")
        return

    if not os.path.exists(watch_folder):
        os.makedirs(watch_folder)

    processed_files = set()
    print(f"\nWatching for new keypoint files in '{watch_folder}'... Press CTRL+C to stop.")

    try:
        while True:
            json_files = glob.glob(os.path.join(watch_folder, '*.json'))
            new_files = set(json_files) - processed_files

            if new_files:
                for file_path in sorted(list(new_files)):
                    print(f"\nNew file detected: {os.path.basename(file_path)}")
                    # Pass the model AND the feature names to the processing function
                    result = process_keypoint_file(file_path, model, feature_names)
                    print(f"  -> Analysis Result: {result}")
                    processed_files.add(file_path)
            
            time.sleep(2)
    except KeyboardInterrupt:
        print("\nStopping the analysis watcher.")

if __name__ == "__main__":
    run_folder_watcher()

