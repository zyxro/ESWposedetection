import os
import pandas as pd
import json
import numpy as np
import glob

# --- This is the feature extraction logic, updated with your suggestions ---
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
    Loads a JSON file, extracts key upper-body keypoints, and calculates 
    the neck angle and the new torso lean angle.
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
    
    left_ear_conf = keypoints.get('left_ear', {}).get('confidence', 0)
    right_ear_conf = keypoints.get('right_ear', {}).get('confidence', 0)
    side = 'left' if left_ear_conf > right_ear_conf else 'right'

    # We only need Ear, Shoulder, and Hip now
    ear = keypoints.get(f'{side}_ear')
    shoulder = keypoints.get(f'{side}_shoulder')
    hip = keypoints.get(f'{side}_hip')

    if not all([ear, shoulder, hip]):
        print(f"  - Skipping {os.path.basename(filepath)}: Missing a required keypoint (ear, shoulder, or hip).")
        return None
    
    # Confidence check for our required upper body points
    min_confidence = 0.3 
    required_parts = {'ear': ear, 'shoulder': shoulder, 'hip': hip}
    
    for part_name, part_data in required_parts.items():
        if part_data['confidence'] < min_confidence:
            print(f"  - Skipping {os.path.basename(filepath)}: Low confidence for '{side}_{part_name}' ({part_data['confidence']:.2f} < {min_confidence}).")
            return None

    ear_coords = [ear['x'], ear['y']]
    shoulder_coords = [shoulder['x'], shoulder['y']]
    hip_coords = [hip['x'], hip['y']]

    # --- CALCULATE NEW ANGLES ---
    # 1. Neck Angle (Ear-Shoulder-Hip) remains the same and is a great feature.
    neck_angle = calculate_angle(ear_coords, shoulder_coords, hip_coords)

    # 2. Torso Lean Angle (Your suggestion)
    # Create a horizontal reference point from the hip
    horizontal_ref_point = [hip['x'] + 100, hip['y']] # 100 pixels to the right on the same horizontal line
    torso_lean_angle = calculate_angle(shoulder_coords, hip_coords, horizontal_ref_point)

    features = {
        "neck_angle": neck_angle,
        "torso_lean_angle": torso_lean_angle # New feature name
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
            output_csv_path = 'posture_dataset.csv'
            posture_dataset.to_csv(output_csv_path, index=False)
            
            print(f"\n\nSuccessfully created dataset with {len(posture_dataset)} samples.")
            print(f"Dataset saved to: {output_csv_path}")
            
            print("\n--- Dataset Summary ---")
            print(posture_dataset['label'].value_counts())
            print("\n--- Sample Data ---")
            print(posture_dataset.head())
            print("-------------------")

