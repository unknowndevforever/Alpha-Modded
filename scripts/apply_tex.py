from PIL import Image
t=Image.open('/home/claude/w/jar/terrain.png').convert('RGBA')
for tile,f in ((129,'rail_off16.png'),(130,'rail_on16.png')):
    img=Image.open('/home/claude/w/'+f); x=(tile%16)*16; y=(tile//16)*16
    t.paste((0,0,0,0),(x,y,x+16,y+16)); t.paste(img,(x,y))
t.save('/home/claude/w/pkg/terrain.png')
