class UserProfile {
  final String userId;
  final String name;
  final int age;
  final String address;
  final String email;
  final String photoUrl;
  final String userType;
  final String phoneNumber;
  final String medicalConditions;
  final String emergencyContact;

  UserProfile({
    required this.userId,
    required this.name,
    required this.age,
    required this.address,
    required this.email,
    required this.photoUrl,
    required this.userType,
    required this.phoneNumber,
    required this.medicalConditions,
    required this.emergencyContact,
  });

  // Empty constructor for Firestore
  UserProfile.empty()
    : userId = '',
      name = '',
      age = 0,
      address = '',
      email = '',
      photoUrl = '',
      userType = '',
      phoneNumber = '',
      medicalConditions = '',
      emergencyContact = '';

  // From Firestore document
  factory UserProfile.fromMap(Map<String, dynamic> map) {
    return UserProfile(
      userId: map['userId'] ?? '',
      name: map['name'] ?? '',
      age: map['age'] ?? 0,
      address: map['address'] ?? '',
      email: map['email'] ?? '',
      photoUrl: map['photoUrl'] ?? '',
      userType: map['userType'] ?? '',
      phoneNumber: map['phoneNumber'] ?? '',
      medicalConditions: map['medicalConditions'] ?? '',
      emergencyContact: map['emergencyContact'] ?? '',
    );
  }

  // To Firestore document
  Map<String, dynamic> toMap() {
    return {
      'userId': userId,
      'name': name,
      'age': age,
      'address': address,
      'email': email,
      'photoUrl': photoUrl,
      'userType': userType,
      'phoneNumber': phoneNumber,
      'medicalConditions': medicalConditions,
      'emergencyContact': emergencyContact,
    };
  }

  UserProfile copyWith({
    String? userId,
    String? name,
    int? age,
    String? address,
    String? email,
    String? photoUrl,
    String? userType,
    String? phoneNumber,
    String? medicalConditions,
    String? emergencyContact,
  }) {
    return UserProfile(
      userId: userId ?? this.userId,
      name: name ?? this.name,
      age: age ?? this.age,
      address: address ?? this.address,
      email: email ?? this.email,
      photoUrl: photoUrl ?? this.photoUrl,
      userType: userType ?? this.userType,
      phoneNumber: phoneNumber ?? this.phoneNumber,
      medicalConditions: medicalConditions ?? this.medicalConditions,
      emergencyContact: emergencyContact ?? this.emergencyContact,
    );
  }
}
