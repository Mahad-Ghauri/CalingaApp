import 'package:cloud_firestore/cloud_firestore.dart';
import '../models/user_profile.dart';
import '../models/calinga_pro.dart';

class FirestoreService {
  final FirebaseFirestore _firestore = FirebaseFirestore.instance;

  // Get all CALINGApros
  Future<List<CalingaPro>> getCalingaPros() async {
    try {
      QuerySnapshot querySnapshot = await _firestore
          .collection('users')
          .where('userType', isEqualTo: 'CALINGApro')
          .where('isActive', isEqualTo: true)
          .get();

      return querySnapshot.docs
          .map((doc) => CalingaPro.fromMap(doc.data() as Map<String, dynamic>))
          .toList();
    } catch (e) {
      throw Exception('Failed to get CALINGApros: $e');
    }
  }

  // Search CALINGApros by name or specialization
  Future<List<CalingaPro>> searchCalingaPros(String query) async {
    try {
      QuerySnapshot querySnapshot = await _firestore
          .collection('users')
          .where('userType', isEqualTo: 'CALINGApro')
          .where('isActive', isEqualTo: true)
          .get();

      List<CalingaPro> allPros = querySnapshot.docs
          .map((doc) => CalingaPro.fromMap(doc.data() as Map<String, dynamic>))
          .toList();

      // Filter by query (name or specialization)
      return allPros.where((pro) {
        return pro.name.toLowerCase().contains(query.toLowerCase()) ||
            pro.specialization.toLowerCase().contains(query.toLowerCase());
      }).toList();
    } catch (e) {
      throw Exception('Failed to search CALINGApros: $e');
    }
  }

  // Get CALINGApro by ID
  Future<CalingaPro?> getCalingaProById(String id) async {
    try {
      DocumentSnapshot doc = await _firestore.collection('users').doc(id).get();

      if (doc.exists) {
        return CalingaPro.fromMap(doc.data() as Map<String, dynamic>);
      }
      return null;
    } catch (e) {
      throw Exception('Failed to get CALINGApro: $e');
    }
  }

  // Update user profile
  Future<void> updateUserProfile(UserProfile userProfile) async {
    try {
      await _firestore
          .collection('users')
          .doc(userProfile.userId)
          .update(userProfile.toMap());
    } catch (e) {
      throw Exception('Failed to update user profile: $e');
    }
  }

  // Upload document for CALINGApro verification
  Future<void> uploadDocument(
    String userId,
    String documentType,
    String documentUrl,
  ) async {
    try {
      await _firestore
          .collection('documents')
          .doc(userId)
          .collection('submissions')
          .add({
            'documentType': documentType,
            'documentUrl': documentUrl,
            'uploadedAt': FieldValue.serverTimestamp(),
            'status': 'pending',
          });
    } catch (e) {
      throw Exception('Failed to upload document: $e');
    }
  }

  // Get user documents
  Future<List<Map<String, dynamic>>> getUserDocuments(String userId) async {
    try {
      QuerySnapshot querySnapshot = await _firestore
          .collection('documents')
          .doc(userId)
          .collection('submissions')
          .orderBy('uploadedAt', descending: true)
          .get();

      return querySnapshot.docs
          .map((doc) => {'id': doc.id, ...doc.data() as Map<String, dynamic>})
          .toList();
    } catch (e) {
      throw Exception('Failed to get user documents: $e');
    }
  }
}
