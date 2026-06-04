"use client"

import { useState } from "react"
import Link from "next/link"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card"

interface RegisterFormData {
  tenantName: string
  fullName: string
  email: string
  password: string
}

export function RegisterForm() {
  const [formData, setFormData] = useState<RegisterFormData>({
    tenantName: "",
    fullName: "",
    email: "",
    password: "",
  })
  const [isLoading, setIsLoading] = useState(false)

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setIsLoading(true)
    
    // TODO: Implementar lógica de cadastro
    console.log("Cadastro:", formData)
    
    setTimeout(() => setIsLoading(false), 1000)
  }

  const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const { name, value } = e.target
    setFormData((prev) => ({ ...prev, [name]: value }))
  }

  return (
    <Card className="w-full max-w-md border-0 shadow-xl shadow-primary/5">
      <CardHeader className="space-y-1 text-center">
        <CardTitle className="text-2xl font-bold tracking-tight">
          Crie sua conta
        </CardTitle>
        <CardDescription className="text-muted-foreground">
          Comece seu trial de 14 dias grátis
        </CardDescription>
      </CardHeader>
      <CardContent>
        <form onSubmit={handleSubmit} className="space-y-4">
          <div className="space-y-2">
            <Label htmlFor="tenantName">Nome da Loja/Empresa</Label>
            <Input
              id="tenantName"
              name="tenantName"
              type="text"
              placeholder="Minha Loja Online"
              value={formData.tenantName}
              onChange={handleChange}
              required
              className="h-11"
            />
          </div>
          <div className="space-y-2">
            <Label htmlFor="fullName">Nome Completo</Label>
            <Input
              id="fullName"
              name="fullName"
              type="text"
              placeholder="João Silva"
              value={formData.fullName}
              onChange={handleChange}
              required
              autoComplete="name"
              className="h-11"
            />
          </div>
          <div className="space-y-2">
            <Label htmlFor="email">E-mail</Label>
            <Input
              id="email"
              name="email"
              type="email"
              placeholder="seu@email.com"
              value={formData.email}
              onChange={handleChange}
              required
              autoComplete="email"
              className="h-11"
            />
          </div>
          <div className="space-y-2">
            <Label htmlFor="password">Senha</Label>
            <Input
              id="password"
              name="password"
              type="password"
              placeholder="Mínimo 8 caracteres"
              value={formData.password}
              onChange={handleChange}
              required
              minLength={8}
              autoComplete="new-password"
              className="h-11"
            />
          </div>
          <Button
            type="submit"
            className="w-full h-11 text-base font-medium"
            disabled={isLoading}
          >
            {isLoading ? (
              <span className="flex items-center gap-2">
                <svg
                  className="animate-spin h-4 w-4"
                  xmlns="http://www.w3.org/2000/svg"
                  fill="none"
                  viewBox="0 0 24 24"
                >
                  <circle
                    className="opacity-25"
                    cx="12"
                    cy="12"
                    r="10"
                    stroke="currentColor"
                    strokeWidth="4"
                  />
                  <path
                    className="opacity-75"
                    fill="currentColor"
                    d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"
                  />
                </svg>
                Criando conta...
              </span>
            ) : (
              "Criar conta"
            )}
          </Button>
        </form>
        <p className="mt-4 text-center text-xs text-muted-foreground">
          Ao criar uma conta, você concorda com nossos{" "}
          <Link href="/termos" className="text-primary hover:underline">
            Termos de Uso
          </Link>{" "}
          e{" "}
          <Link href="/privacidade" className="text-primary hover:underline">
            Política de Privacidade
          </Link>
        </p>
        <div className="mt-6 text-center text-sm text-muted-foreground">
          Já possui uma conta?{" "}
          <Link href="/login" className="text-primary font-medium hover:underline">
            Fazer login
          </Link>
        </div>
      </CardContent>
    </Card>
  )
}
