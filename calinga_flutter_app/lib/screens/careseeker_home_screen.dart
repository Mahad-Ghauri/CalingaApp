import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import 'package:provider/provider.dart';
import 'package:cached_network_image/cached_network_image.dart';
import '../services/auth_service.dart';
import '../services/firestore_service.dart';
import '../models/calinga_pro.dart';
import '../models/user_profile.dart';

class CareseekerHomeScreen extends StatefulWidget {
  const CareseekerHomeScreen({super.key});

  @override
  State<CareseekerHomeScreen> createState() => _CareseekerHomeScreenState();
}

class _CareseekerHomeScreenState extends State<CareseekerHomeScreen> {
  final _searchController = TextEditingController();
  List<CalingaPro> _calingaPros = [];
  List<CalingaPro> _filteredPros = [];
  bool _isLoading = true;
  UserProfile? _currentUser;

  @override
  void initState() {
    super.initState();
    _loadData();
  }

  @override
  void dispose() {
    _searchController.dispose();
    super.dispose();
  }

  Future<void> _loadData() async {
    try {
      final authService = Provider.of<AuthService>(context, listen: false);
      final firestoreService = Provider.of<FirestoreService>(
        context,
        listen: false,
      );

      // Load current user profile
      final currentUser = authService.currentUser;
      if (currentUser != null) {
        _currentUser = await authService.getUserProfile(currentUser.uid);
      }

      // Load CALINGApros
      final pros = await firestoreService.getCalingaPros();

      setState(() {
        _calingaPros = pros;
        _filteredPros = pros;
        _isLoading = false;
      });
    } catch (e) {
      setState(() {
        _isLoading = false;
      });
      if (mounted) {
        ScaffoldMessenger.of(
          context,
        ).showSnackBar(SnackBar(content: Text('Error loading data: $e')));
      }
    }
  }

  void _filterPros(String query) {
    setState(() {
      if (query.isEmpty) {
        _filteredPros = _calingaPros;
      } else {
        _filteredPros = _calingaPros.where((pro) {
          return pro.name.toLowerCase().contains(query.toLowerCase()) ||
              pro.specialization.toLowerCase().contains(query.toLowerCase()) ||
              pro.location.toLowerCase().contains(query.toLowerCase());
        }).toList();
      }
    });
  }

  Future<void> _signOut() async {
    try {
      final authService = Provider.of<AuthService>(context, listen: false);
      await authService.signOut();
      if (mounted) {
        context.go('/login');
      }
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(
          context,
        ).showSnackBar(SnackBar(content: Text('Error signing out: $e')));
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: Colors.grey[50],
      appBar: AppBar(
        backgroundColor: Theme.of(context).primaryColor,
        foregroundColor: Colors.white,
        title: const Text(
          'Find Care',
          style: TextStyle(fontFamily: 'SFPro', fontWeight: FontWeight.w600),
        ),
        actions: [
          IconButton(
            icon: const Icon(Icons.person),
            onPressed: () => context.go('/profile'),
          ),
        ],
      ),
      drawer: Drawer(
        child: ListView(
          padding: EdgeInsets.zero,
          children: [
            UserAccountsDrawerHeader(
              decoration: BoxDecoration(color: Theme.of(context).primaryColor),
              accountName: Text(
                _currentUser?.name ?? 'User',
                style: const TextStyle(
                  fontFamily: 'SFPro',
                  fontWeight: FontWeight.w600,
                ),
              ),
              accountEmail: Text(
                _currentUser?.email ?? '',
                style: const TextStyle(fontFamily: 'SFPro'),
              ),
              currentAccountPicture: CircleAvatar(
                backgroundColor: Colors.white,
                child: _currentUser?.photoUrl.isNotEmpty == true
                    ? CachedNetworkImage(
                        imageUrl: _currentUser!.photoUrl,
                        imageBuilder: (context, imageProvider) => Container(
                          decoration: BoxDecoration(
                            shape: BoxShape.circle,
                            image: DecorationImage(
                              image: imageProvider,
                              fit: BoxFit.cover,
                            ),
                          ),
                        ),
                        placeholder: (context, url) =>
                            const CircularProgressIndicator(),
                        errorWidget: (context, url, error) => Icon(
                          Icons.person,
                          color: Theme.of(context).primaryColor,
                          size: 40,
                        ),
                      )
                    : Icon(
                        Icons.person,
                        color: Theme.of(context).primaryColor,
                        size: 40,
                      ),
              ),
            ),
            ListTile(
              leading: const Icon(Icons.home),
              title: const Text('Home'),
              onTap: () => Navigator.pop(context),
            ),
            ListTile(
              leading: const Icon(Icons.person),
              title: const Text('Profile'),
              onTap: () {
                Navigator.pop(context);
                context.go('/profile');
              },
            ),
            ListTile(
              leading: const Icon(Icons.history),
              title: const Text('Booking History'),
              onTap: () => Navigator.pop(context),
            ),
            ListTile(
              leading: const Icon(Icons.help),
              title: const Text('Help & Support'),
              onTap: () => Navigator.pop(context),
            ),
            const Divider(),
            ListTile(
              leading: const Icon(Icons.logout, color: Colors.red),
              title: const Text(
                'Sign Out',
                style: TextStyle(color: Colors.red),
              ),
              onTap: _signOut,
            ),
          ],
        ),
      ),
      body: Column(
        children: [
          // Search bar
          Container(
            padding: const EdgeInsets.all(16),
            color: Colors.white,
            child: TextField(
              controller: _searchController,
              onChanged: _filterPros,
              decoration: InputDecoration(
                hintText: 'Search caregivers...',
                prefixIcon: const Icon(Icons.search),
                border: OutlineInputBorder(
                  borderRadius: BorderRadius.circular(25),
                  borderSide: BorderSide.none,
                ),
                filled: true,
                fillColor: Colors.grey[100],
                contentPadding: const EdgeInsets.symmetric(
                  horizontal: 20,
                  vertical: 15,
                ),
              ),
            ),
          ),

          // Content
          Expanded(
            child: _isLoading
                ? const Center(child: CircularProgressIndicator())
                : _filteredPros.isEmpty
                ? Center(
                    child: Column(
                      mainAxisAlignment: MainAxisAlignment.center,
                      children: [
                        Icon(
                          Icons.search_off,
                          size: 64,
                          color: Colors.grey[400],
                        ),
                        const SizedBox(height: 16),
                        Text(
                          'No caregivers found',
                          style: TextStyle(
                            fontSize: 18,
                            color: Colors.grey[600],
                            fontFamily: 'SFPro',
                          ),
                        ),
                        const SizedBox(height: 8),
                        Text(
                          'Try adjusting your search criteria',
                          style: TextStyle(
                            color: Colors.grey[500],
                            fontFamily: 'SFPro',
                          ),
                        ),
                      ],
                    ),
                  )
                : RefreshIndicator(
                    onRefresh: _loadData,
                    child: ListView.builder(
                      padding: const EdgeInsets.all(16),
                      itemCount: _filteredPros.length,
                      itemBuilder: (context, index) {
                        final pro = _filteredPros[index];
                        return _buildCaregiverCard(pro);
                      },
                    ),
                  ),
          ),
        ],
      ),
    );
  }

  Widget _buildCaregiverCard(CalingaPro pro) {
    return Card(
      margin: const EdgeInsets.only(bottom: 16),
      elevation: 2,
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
      child: InkWell(
        borderRadius: BorderRadius.circular(12),
        onTap: () {
          // Navigate to caregiver details
          // context.go('/caregiver-details/${pro.id}');
        },
        child: Padding(
          padding: const EdgeInsets.all(16),
          child: Row(
            children: [
              // Profile image
              CircleAvatar(
                radius: 30,
                backgroundColor: Colors.grey[200],
                child: pro.photoUrl.isNotEmpty
                    ? CachedNetworkImage(
                        imageUrl: pro.photoUrl,
                        imageBuilder: (context, imageProvider) => Container(
                          decoration: BoxDecoration(
                            shape: BoxShape.circle,
                            image: DecorationImage(
                              image: imageProvider,
                              fit: BoxFit.cover,
                            ),
                          ),
                        ),
                        placeholder: (context, url) =>
                            const CircularProgressIndicator(),
                        errorWidget: (context, url, error) =>
                            const Icon(Icons.person, size: 30),
                      )
                    : const Icon(Icons.person, size: 30),
              ),
              const SizedBox(width: 16),

              // Details
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      pro.name,
                      style: const TextStyle(
                        fontSize: 18,
                        fontWeight: FontWeight.w600,
                        fontFamily: 'SFPro',
                      ),
                    ),
                    const SizedBox(height: 4),
                    Text(
                      pro.specialization,
                      style: TextStyle(
                        fontSize: 14,
                        color: Theme.of(context).primaryColor,
                        fontFamily: 'SFPro',
                      ),
                    ),
                    const SizedBox(height: 4),
                    Row(
                      children: [
                        Icon(
                          Icons.location_on,
                          size: 16,
                          color: Colors.grey[600],
                        ),
                        const SizedBox(width: 4),
                        Expanded(
                          child: Text(
                            pro.location,
                            style: TextStyle(
                              fontSize: 12,
                              color: Colors.grey[600],
                              fontFamily: 'SFPro',
                            ),
                          ),
                        ),
                      ],
                    ),
                    const SizedBox(height: 8),
                    Row(
                      children: [
                        // Rating
                        Row(
                          children: [
                            const Icon(
                              Icons.star,
                              color: Colors.amber,
                              size: 16,
                            ),
                            const SizedBox(width: 4),
                            Text(
                              pro.rating.toStringAsFixed(1),
                              style: const TextStyle(
                                fontSize: 12,
                                fontWeight: FontWeight.w500,
                                fontFamily: 'SFPro',
                              ),
                            ),
                          ],
                        ),
                        const Spacer(),
                        // Hourly rate
                        Text(
                          '\$${pro.hourlyRate.toStringAsFixed(0)}/hr',
                          style: TextStyle(
                            fontSize: 14,
                            fontWeight: FontWeight.w600,
                            color: Theme.of(context).primaryColor,
                            fontFamily: 'SFPro',
                          ),
                        ),
                      ],
                    ),
                  ],
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}
