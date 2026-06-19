import { redirect } from 'next/navigation'
import type { Locale } from '@/shared/i18n/config'

interface Props {
  params: Promise<{ lang: Locale }>
}

export default async function RootPage({ params }: Props) {
  const { lang } = await params
  redirect(`/${lang}/dashboard`)
}
