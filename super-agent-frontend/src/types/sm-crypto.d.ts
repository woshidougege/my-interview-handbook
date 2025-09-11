declare module 'sm-crypto' {
  export const sm2: {
    doEncrypt: (data: string, publicKey: string, cipherMode: number) => string;
    doDecrypt: (encryptedData: string, privateKey: string, cipherMode: number) => string;
    generateKeyPairHex: () => { privateKey: string; publicKey: string };
  };

  export const sm3: {
    sum: (data: string | number[] | ArrayBuffer) => string;
  };

  export const sm4: {
    encrypt: (data: string, key: string, options?: any) => string;
    decrypt: (encryptedData: string, key: string, options?: any) => string;
  };
}