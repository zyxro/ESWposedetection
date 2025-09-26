import os
import pandas as pd
import json
import numpy as np
import glob

# --- This is the feature extraction logic, updated with your specifications ---
def calculate_angle(a, b, c):
    """Calculates the angle between three points in 2D space."""
    a = np.array(a)
    b = np.array(b)
    c = np.array(c)
    ba = a - b
    bc = c - b
    dot_product = np.dot(ba, bc)
    magnitude_ba = np.linalg.norm(ba)
    magnitude_bc = np.linalg.norm(bc)
    cosine_angle = dot_product / (magnitude_ba * magnitude_bc + 1e-6)
    cosine_angle = np.clip(cosine_angle, -1.0, 1.0)
    angle = np.arccos(cosine_angle)
    return np.degrees(angle)

def extract_features_from_file(filepath):
    """
    Loads a JSON file, extracts keypoints for the right side of the body, 
    and calculates the three angles you defined.
    """
    try:
        with open(filepath, 'r') as f:
            data = json.load(f)
    except Exception as e:
        print(f"  - Skipping {os.path.basename(filepath)} due to read error: {e}")
        return None

    if not data.get('persons'):
        print(f"  - Skipping {os.path.basename(filepath)}: No persons detected.")
        return None

    keypoints = {part['part_name']: part for part in data['persons'][0]['keypoints']}
    
    # --- We are now specifically targeting the RIGHT side keypoints ---
    ear = keypoints.get('right_ear')
    shoulder = keypoints.get('right_shoulder')
    hip = keypoints.get('right_hip')
    knee = keypoints.get('right_knee')
    ankle = keypoints.get('right_ankle')

    # Check if all the necessary keypoints were found in the file
    if not all([ear, shoulder, hip, knee, ankle]):
        missing = [k for k, v in {'ear': ear, 'shoulder': shoulder, 'hip': hip, 'knee': knee, 'ankle': ankle}.items() if v is None]
        print(f"  - Skipping {os.path.basename(filepath)}: Missing keypoint(s): {', '.join(missing)}")
        return None
    
    # # --- Confidence check commented out as requested ---
    # # You can re-enable this later by removing the '#' from the lines below
    # # if you want to filter for higher quality data.
    # min_confidence = 0.3 
    # required_parts = {'ear': ear, 'shoulder': shoulder, 'hip': hip, 'knee': knee, 'ankle': ankle}
    # for part_name, part_data in required_parts.items():
    #     if part_data['confidence'] < min_confidence:
    #         print(f"  - Skipping {os.path.basename(filepath)}: Low confidence for 'right_{part_name}' ({part_data['confidence']:.2f} < {min_confidence}).")
    #         return None

    # Get the coordinates for each point
    ear_coords = [ear['x'], ear['y']]
    shoulder_coords = [shoulder['x'], shoulder['y']]
    hip_coords = [hip['x'], hip['y']]
    knee_coords = [knee['x'], knee['y']]
    ankle_coords = [ankle['x'], ankle['y']]

    # --- CALCULATE THE THREE ANGLES YOU DEFINED ---
    # 1. Angle at the back of the knees (Ankle-Knee-Hip)
    knee_angle = calculate_angle(ankle_coords, knee_coords, hip_coords)

    # 2. Angle at the front of the torso/hip (Knee-Hip-Shoulder)
    torso_hip_angle = calculate_angle(knee_coords, hip_coords, shoulder_coords)
    
    # 3. Angle at the neck/torso junction (Hip-Shoulder-Ear)
    neck_torso_angle = calculate_angle(hip_coords, shoulder_coords, ear_coords)

    features = {
        "knee_angle": knee_angle,
        "torso_hip_angle": torso_hip_angle,
        "neck_torso_angle": neck_torso_angle
    }
    return features
# --------------------------------------------------------------------


def build_dataset(base_dir):
    """
    Traverses subdirectories of a base directory, processes JSON files,
    and creates a labeled dataset.
    """
    all_features = []
    posture_folders = glob.glob(os.path.join(base_dir, '*/'))

    if not posture_folders:
        print(f"Error: No posture subdirectories found in '{base_dir}'.")
        print("Please ensure your directory structure is like 'webcam_captures/json/hunchback/...'")
        return None

    for posture_dir in posture_folders:
        posture_label = os.path.basename(os.path.normpath(posture_dir))
        print(f"\nProcessing posture: {posture_label}")
        
        json_files = glob.glob(os.path.join(posture_dir, '*.json'))
        
        if not json_files:
            print(f"  - No JSON files found in {posture_dir}")
            continue

        for json_file in json_files:
            features = extract_features_from_file(json_file)
            if features:
                features['label'] = posture_label
                features['source_file'] = os.path.basename(json_file)
                all_features.append(features)
    
    if not all_features:
        print("No features could be extracted. The dataset is empty.")
        return None

    return pd.DataFrame(all_features)

# --- MAIN EXECUTION ---
if __name__ == "__main__":
    json_base_directory = 'webcam_captures/json'

    if not os.path.isdir(json_base_directory):
        print(f"Error: The directory '{json_base_directory}' does not exist.")
    else:
        posture_dataset = build_dataset(json_base_directory)

        if posture_dataset is not None and not posture_dataset.empty:
            output_csv_path = 'posture_dataset_final.csv'
            posture_dataset.to_csv(output_csv_path, index=False)
            
            print(f"\n\nSuccessfully created dataset with {len(posture_dataset)} samples.")
            print(f"Dataset saved to: {output_csv_path}")
            
            print("\n--- Dataset Summary ---")
            print(posture_dataset['label'].value_counts())
            print("\n--- Sample Data ---")
            print(posture_dataset.head())
            print("-------------------")

