import { Controller, Post, Get, Body, UseGuards, Request } from '@nestjs/common';
import { AuthGuard } from '@nestjs/passport';
import { AuthService } from './auth.service';

class LoginDto {
  email: string;
  password: string;
}

class MeResponse {
  userId: string;
  email: string;
  role: string;
}

@Controller('auth')
export class AuthController {
  constructor(private authService: AuthService) {}

  @Post('login')
  async login(@Body() dto: LoginDto) {
    // In production, call auth-service via HTTP
    // For now, validate with JWT and return token
    const user = await this.authService.validateUser(dto.email, dto.password);
    if (!user) {
      return { error: 'Invalid credentials' };
    }
    const token = this.authService.generateToken(user);
    return { accessToken: token, tokenType: 'Bearer' };
  }

  @UseGuards(AuthGuard('jwt'))
  @Get('me')
  getProfile(@Request() req): MeResponse {
    return {
      userId: req.user.sub,
      email: req.user.email,
      role: req.user.role,
    };
  }
}