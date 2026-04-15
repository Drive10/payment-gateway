import { Injectable, UnauthorizedException } from '@nestjs/common';
import { JwtService } from '@nestjs/jwt';
import * as bcrypt from 'bcrypt';

export interface UserPayload {
  sub: string;
  email: string;
  role: string;
}

@Injectable()
export class AuthService {
  constructor(private jwtService: JwtService) {}

  async validateUser(email: string, password: string): Promise<UserPayload | null> {
    // In production, this would query the auth-service via HTTP
    // For now, we support basic auth with JWT validation
    return null;
  }

  generateToken(user: UserPayload): string {
    return this.jwtService.sign(user);
  }

  verifyToken(token: string): UserPayload {
    try {
      return this.jwtService.verify(token);
    } catch {
      throw new UnauthorizedException('Invalid or expired token');
    }
  }
}