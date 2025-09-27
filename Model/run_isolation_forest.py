import os
import time
import glob
import json
import numpy as np
import pandas as pd
from sklearn.ensemble import IsolationForest

# --- Helper function to calculate angles (from previous scripts) ---
def calculate_angle(a, b, c):
    """Calculates the angle between three points in 2D space, with 'b' as the vertex."""
    a, b, c = np.array(a), np.array(b), np.array(c)
    ba, bc = a - b, c - b
    dot_product = np.dot(ba, bc)
    magnitude_ba = np.linalg.norm(ba)
    magnitude_bc = np.linalg.norm(bc)
    cosine_angle = np.clip(dot_product / (magnitude_ba * magnitude_bc + 1e-6), -1.0, 1.0)
    return np.degrees(np.arccos(cosine_angle))

def extract_features_from_file(filepath):
    """Loads a JSON file and extracts the three key posture angles."""
    try:
        with open(filepath, 'r') as f:
            data = json.load(f)
    except Exception:
        return None

    if not data.get('persons'):
        return None

    keypoints = {part['part_name']: part for part in data['persons'][0]['keypoints']}
    side = 'left' if keypoints.get('left_ear', {}).get('confidence', 0) > keypoints.get('right_ear', {}).get('confidence', 0) else 'right'
    
    required_parts = {
        'ear': keypoints.get(f'{side}_ear'), 'shoulder': keypoints.get(f'{side}_shoulder'),
        'hip': keypoints.get(f'{side}_hip'), 'knee': keypoints.get(f'{side}_knee'),
        'ankle': keypoints.get(f'{side}_ankle')
    }
    
    if not all(required_parts.values()): return None
    coords = {k: [v['x'], v['y']] for k, v in required_parts.items()}

    neck_angle = calculate_angle(coords['hip'], coords['shoulder'], coords['ear'])
    back_angle = calculate_angle(coords['knee'], coords['hip'], coords['shoulder'])
    legs_angle = calculate_angle(coords['ankle'], coords['knee'], coords['hip'])
    
    return [neck_angle, back_angle, legs_angle]

# --- 1. Training the Anomaly Detection Model ---
def train_anomaly_model(good_posture_dir='webcam_captures/json/straight'):
    """Trains an Isolation Forest model only on the good posture data."""
    if not os.path.isdir(good_posture_dir):
        print(f"Error: Directory with good postures '{good_posture_dir}' not found.")
        return None
    
    print("--- Training Anomaly Detection Model ---")
    print(f"Loading 'good' posture data from: {good_posture_dir}")
    
    good_posture_features = []
    json_files = glob.glob(os.path.join(good_posture_dir, '*.json'))
    for file_path in json_files:
        features = extract_features_from_file(file_path)
        if features:
            good_posture_features.append(features)
            
    if len(good_posture_features) < 1:
        print("Error: No valid 'good' posture files found to train the model.")
        return None
        
    df = pd.DataFrame(good_posture_features, columns=['neck_angle', 'back_angle', 'legs_angle'])
    
    # Initialize and train the Isolation Forest
    # `contamination` is an estimate of the proportion of outliers in the data. 
    # Since this is our 'good' data, we set it low.
    model = IsolationForest(n_estimators=100, contamination=0.01, random_state=42)
    model.fit(df)
    
    print(f"Model trained on {len(df)} samples of good posture.")
    print("--------------------------------------\n")
    return model

# --- 2. Main Application: Folder Watcher ---
def run_posture_analysis(model, watch_folder='keypoints_to_analyze'):
    """Watches a folder and analyzes new files using the trained model."""
    if not model:
        return
        
    if not os.path.exists(watch_folder):
        os.makedirs(watch_folder)

    processed_files = set()
    print(f"Watching for new keypoint files in '{watch_folder}'... Press CTRL+C to stop.")

    while True:
        try:
            json_files = glob.glob(os.path.join(watch_folder, '*.json'))
            new_files = sorted(list(set(json_files) - processed_files))

            if new_files:
                for file_path in new_files:
                    features = extract_features_from_file(file_path)
                    if features:
                        # The model gives a decision_function score. 
                        # Negative scores are anomalies (bad posture).
                        score = model.decision_function([features])[0]
                        prediction = model.predict([features])[0] # Returns 1 for normal, -1 for anomaly
                        
                        status = "GOOD" if prediction == 1 else "INCORRECT"
                        
                        print(f"\n--- Analyzing: {os.path.basename(file_path)} ---")
                        print(f"  Neck Angle: {features[0]:.1f}°")
                        print(f"  Back Angle: {features[1]:.1f}°")
                        print(f"  Legs Angle: {features[2]:.1f}°")
                        print(f"  Posture Score: {score:.3f} (closer to 0 is better, negative is bad)")
                        print(f"  STATUS: {status}")

                    else:
                        print(f"\nCould not process file: {os.path.basename(file_path)}")
                    
                    processed_files.add(file_path)
            
            time.sleep(2)
        except KeyboardInterrupt:
            print("\nStopping the analysis.")
            break
        except Exception as e:
            print(f"An error occurred: {e}")
            time.sleep(5)


if __name__ == "__main__":
    # First, train the model using your reference "straight" posture files
    anomaly_model = train_anomaly_model(good_posture_dir='webcam_captures/json/straight')
    
    # Then, start the folder watcher to analyze new files
    run_posture_analysis(anomaly_model, watch_folder='keypoints_to_analyze')
