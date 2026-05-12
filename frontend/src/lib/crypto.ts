import CryptoJS from 'crypto-js'

const AES_KEY = CryptoJS.enc.Utf8.parse(import.meta.env.VITE_AES_KEY as string)

export function encryptPassword(password: string): string {
  return CryptoJS.AES.encrypt(password, AES_KEY, {
    mode: CryptoJS.mode.ECB,
    padding: CryptoJS.pad.Pkcs7,
  }).toString()
}
