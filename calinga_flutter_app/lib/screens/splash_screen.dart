import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import 'package:provider/provider.dart';
import '../services/auth_service.dart';

class SplashScreen extends StatefulWidget {
  const SplashScreen({super.key});

  @override
  State<SplashScreen> createState() => _SplashScreenState();
}

class _SplashScreenState extends State<SplashScreen> {
  @override
  void initState() {
    super.initState();
    _navigateToNextScreen();
  }

  Future<void> _navigateToNextScreen() async {
    // Wait for 2 seconds (like the Android version)
    await Future.delayed(const Duration(seconds: 2));

    if (!mounted) return;

    try {
      final authService = Provider.of<AuthService>(context, listen: false);
      final currentUser = authService.currentUser;

      if (currentUser != null) {
        // User is logged in, get their profile to determine user type
        final userProfile = await authService.getUserProfile(currentUser.uid);
        if (mounted && userProfile != null) {
          if (userProfile.userType == 'CALINGApro') {
            context.go('/caregiver-home');
          } else {
            context.go('/careseeker-home');
          }
        } else if (mounted) {
          context.go('/login');
        }
      } else if (mounted) {
        // User is not logged in
        context.go('/login');
      }
    } catch (e) {
      if (mounted) {
        context.go('/login');
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: Colors.white,
      body: Center(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            // App Logo
            Image.asset(
              'assets/images/calinga_logo.png',
              width: 200,
              height: 200,
              errorBuilder: (context, error, stackTrace) {
                return Container(
                  width: 200,
                  height: 200,
                  decoration: BoxDecoration(
                    color: Theme.of(context).primaryColor,
                    borderRadius: BorderRadius.circular(100),
                  ),
                  child: const Icon(
                    Icons.health_and_safety,
                    size: 100,
                    color: Colors.white,
                  ),
                );
              },
            ),
            const SizedBox(height: 30),
            // App Name
            Text(
              'CalingaApp',
              style: TextStyle(
                fontSize: 32,
                fontWeight: FontWeight.bold,
                color: Theme.of(context).primaryColor,
                fontFamily: 'SFPro',
              ),
            ),
            const SizedBox(height: 10),
            // Tagline
            Text(
              'Connecting Care, Building Trust',
              style: TextStyle(
                fontSize: 16,
                color: Colors.grey[600],
                fontFamily: 'SFPro',
              ),
            ),
            const SizedBox(height: 50),
            // Loading indicator
            CircularProgressIndicator(
              valueColor: AlwaysStoppedAnimation<Color>(
                Theme.of(context).primaryColor,
              ),
            ),
          ],
        ),
      ),
    );
  }
}
