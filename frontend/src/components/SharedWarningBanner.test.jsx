import { render, screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import { SharedWarningBanner } from './SharedWarningBanner'

describe('SharedWarningBanner', () => {
  it('displays the shared conversation warning', () => {
    render(<SharedWarningBanner />)
    expect(screen.getByText('Conversations are shared. Do not enter sensitive information.')).toBeVisible()
  })
})

