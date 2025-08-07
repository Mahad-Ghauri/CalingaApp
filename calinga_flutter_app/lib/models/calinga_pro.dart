class CalingaPro {
  final String id;
  final String name;
  final String specialization;
  final String location;
  final double rating;
  final String photoUrl;
  final bool isActive;
  final String description;
  final List<String> services;
  final double hourlyRate;
  final String experience;
  final List<String> certifications;

  CalingaPro({
    required this.id,
    required this.name,
    required this.specialization,
    required this.location,
    required this.rating,
    required this.photoUrl,
    required this.isActive,
    required this.description,
    required this.services,
    required this.hourlyRate,
    required this.experience,
    required this.certifications,
  });

  factory CalingaPro.fromMap(Map<String, dynamic> map) {
    return CalingaPro(
      id: map['id'] ?? '',
      name: map['name'] ?? '',
      specialization: map['specialization'] ?? '',
      location: map['location'] ?? '',
      rating: (map['rating'] ?? 0.0).toDouble(),
      photoUrl: map['photoUrl'] ?? '',
      isActive: map['isActive'] ?? false,
      description: map['description'] ?? '',
      services: List<String>.from(map['services'] ?? []),
      hourlyRate: (map['hourlyRate'] ?? 0.0).toDouble(),
      experience: map['experience'] ?? '',
      certifications: List<String>.from(map['certifications'] ?? []),
    );
  }

  Map<String, dynamic> toMap() {
    return {
      'id': id,
      'name': name,
      'specialization': specialization,
      'location': location,
      'rating': rating,
      'photoUrl': photoUrl,
      'isActive': isActive,
      'description': description,
      'services': services,
      'hourlyRate': hourlyRate,
      'experience': experience,
      'certifications': certifications,
    };
  }
}
